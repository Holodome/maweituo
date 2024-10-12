package maweituo
package http
package routes
package ads
import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class AdChatRoutes[F[_]: MonadThrow](chatService: ChatService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
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
      },
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "chats")
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => adId =>
        given Identity = Identity(authed.id)
        chatService.create(adId).void
          .toOut
      }
  )
