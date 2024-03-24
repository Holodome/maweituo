package com.holodome.http.routes

import cats.MonadThrow
import com.holodome.domain.users.AuthedUser
import com.holodome.services.AuthService
import dev.profunktor.auth.AuthHeaders
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import cats.syntax.all._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.AuthMiddleware

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])
    extends Http4sDsl[F] {

  val httpRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(user.id, _)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    authMiddleware(httpRoutes)
}
