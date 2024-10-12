package maweituo
package http
package routes
package ads

import cats.Monad
import cats.syntax.all.*

import maweituo.domain.all.*

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

    case POST -> Root / "ads" / AdIdVar(adId) / "chats" as user =>
      given Identity = Identity(user.id)
      chatService.create(adId) *> Created()
  }
