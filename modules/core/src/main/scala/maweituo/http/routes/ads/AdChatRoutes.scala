package maweituo
package http
package routes
package ads

import maweituo.domain.all.*

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl

final class AdChatRoutes[F[_]: Monad](chatService: ChatService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root / "ads" / AdIdVar(_) / "chats" / ChatIdVar(chatId) as user =>
      given Identity = Identity(user.id)
      chatService
        .get(chatId)
        .map(ChatDto.fromDomain)
        .flatMap(Ok(_))

    case GET -> Root / "ads" / AdIdVar(adId) / "myChat" as user =>
      given Identity = Identity(user.id)
      chatService
        .findForAdAndUser(adId)
        .value
        .flatMap {
          case Some(chat) => Ok(ChatDto.fromDomain(chat))
          case None       => Ok(Json.obj(("errors", "chat not found".asJson)))
        }

    case POST -> Root / "ads" / AdIdVar(adId) / "chats" as user =>
      given Identity = Identity(user.id)
      chatService.create(adId) *> Created()
  }
