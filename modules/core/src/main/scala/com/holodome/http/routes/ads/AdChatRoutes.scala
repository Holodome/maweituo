package com.holodome.http.routes.ads

import cats.effect.Concurrent
import cats.syntax.all._
import cats.{Monad, MonadThrow}
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.AuthedUser
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.http.vars.AdIdVar
import com.holodome.services._
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdChatRoutes[F[_]: Monad](
    chatService: ChatService[F]
)(implicit
    H: HttpErrorHandler[F, ApplicationError]
) extends Http4sDsl[F] {

  private val prefixPath = "/ads"

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {

    case GET -> Root / AdIdVar(adId) / "chat" as user =>
      chatService
        .findForAdAndUser(adId, user.id)
        .fold(Ok(Json.obj(("errors", "chat not found".asJson))))(id => Ok(id))
        .flatten

    case POST -> Root / AdIdVar(adId) / "chat" as user =>
      chatService
        .create(adId, user.id)
        .flatMap(Ok(_))
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes)))))
}
