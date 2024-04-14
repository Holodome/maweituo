package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users.AuthedUser
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.services.AuthService
import dev.profunktor.auth.AuthHeaders
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])(implicit
    H: HttpErrorHandler[F, ApplicationError]
) extends Http4sDsl[F] {

  private val httpRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(user.id, _)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(H.handle(authMiddleware(httpRoutes))))
}
