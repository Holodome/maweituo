package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.http.dto.AdChatsResponseDto

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdChatEndpointDefs(using builder: EndpointBuilderDefs):
  val `get /ads/$adId/chats/$chatId` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id"))
      .out(jsonBody[ChatDto])

  val `get /ads/$adId/chats` =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(jsonBody[AdChatsResponseDto])

  val `post /ads/$adId/chats` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(statusCode(StatusCode.Created))

final class AdChatEndpoints[F[_]: MonadThrow](chatService: ChatService[F])(using EndpointsBuilder[F])
    extends AdChatEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /ads/$adId/chats/$chatId`.secure.serverLogic { authed => (_, chatId) =>
      given Identity = Identity(authed.id)
      chatService
        .get(chatId)
        .map(ChatDto.fromDomain)
        .toOut
    },
    `get /ads/$adId/chats`.secure.serverLogic { authed => adId =>
      given Identity = Identity(authed.id)
      chatService
        .findForAd(adId)
        .map(x => AdChatsResponseDto(adId, x.map(ChatDto.fromDomain)))
        .toOut
    },
    `post /ads/$adId/chats`.secure.serverLogic { authed => adId =>
      given Identity = Identity(authed.id)
      chatService.create(adId).void
        .toOut
    }
  ).map(_.tag("ads"))
