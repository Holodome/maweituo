package com.holodome.http.routes.ads

import cats.Monad
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.services.ChatService
import com.holodome.domain.users.AuthedUser
import com.holodome.http.vars.AdIdVar
import com.holodome.http.{HttpErrorHandler, Routes}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import com.holodome.http.vars.ChatIdVar

final case class AdChatRoutes[F[_]: Monad](
    chatService: ChatService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {

    case GET -> Root / AdIdVar(_) / "chat" / ChatIdVar(chatId) as user =>
      chatService
        .get(chatId, user.id)
        .flatMap(Ok(_))

    case GET -> Root / AdIdVar(adId) / "myChat" as user =>
      chatService
        .findForAdAndUser(adId, user.id)
        .fold(Ok(Json.obj(("errors", "chat not found".asJson))))(id => Ok(id))
        .flatten

    case POST -> Root / AdIdVar(adId) / "chat" as user =>
      chatService
        .create(adId, user.id)
        .flatMap(Ok(_))
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] =
    Routes(None, Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes)))))
}
