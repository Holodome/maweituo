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

final class AdChatEndpoints[F[_]: MonadThrow](chatService: ChatService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getChatEndpoint =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats" / path[ChatId]("chat_id"))
      .out(jsonBody[ChatDto])
      .serverLogic { authed => (_, chatId) =>
        given Identity = Identity(authed.id)
        chatService
          .get(chatId)
          .map(ChatDto.fromDomain)
          .toOut
      }

  val getChatsEndpoint =
    builder.authed
      .get
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(jsonBody[AdChatsResponseDto])
      .serverLogic { authed => adId =>
        given Identity = Identity(authed.id)
        chatService
          .findForAd(adId)
          .map(x => AdChatsResponseDto(adId, x.map(ChatDto.fromDomain)))
          .toOut
      }

  val createChatEndpoint =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => adId =>
        given Identity = Identity(authed.id)
        chatService.create(adId).void
          .toOut
      }

  override val endpoints = List(
    getChatEndpoint,
    getChatsEndpoint,
    createChatEndpoint
  ).map(_.tag("ads"))
