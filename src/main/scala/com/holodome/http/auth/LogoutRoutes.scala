package com.holodome.http.auth

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.users._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.services.AuthService
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.auth.AuthHeaders
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])
    extends Http4sDsl[F] {
  private val httpRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(user.name, _)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] = authMiddleware(
    httpRoutes
  )
}
