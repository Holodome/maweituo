package maweituo
package http
package endpoints
package ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.http.dto.AdChatsResponseDto

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdChatEndpointDefs(using builder: EndpointBuilderDefs):
  def `get /ads/$adId/chats/$chatId` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id"))
      .out(jsonBody[ChatDto])

  def `get /ads/$adId/chats` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(jsonBody[AdChatsResponseDto])

  def `post /ads/$adId/chats` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(jsonBody[CreateChatResponseDto])
      .out(statusCode(StatusCode.Created))

final class AdChatEndpoints[F[_]: MonadThrow](chatService: ChatService[F])(using EndpointsBuilder[F])
    extends AdChatEndpointDefs with Endpoints[F]:

  override def endpoints = List(
    `get /ads/$adId/chats/$chatId`.authedServerLogic { (_, chatId) =>
      chatService
        .get(chatId)
        .map(ChatDto.fromDomain)
    },
    `get /ads/$adId/chats`.authedServerLogic { adId =>
      chatService
        .findForAd(adId)
        .map(x => AdChatsResponseDto(adId, x.map(ChatDto.fromDomain)))
    },
    `post /ads/$adId/chats`.authedServerLogic { adId =>
      chatService.create(adId).map(CreateChatResponseDto(_))
    }
  ).map(_.tag("ads"))
