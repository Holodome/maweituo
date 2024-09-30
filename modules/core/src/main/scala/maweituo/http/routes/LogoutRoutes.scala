package maweituo.http.routes

import maweituo.domain.users.AuthedUser
import maweituo.domain.users.services.AuthService
import maweituo.http.Routes

import cats.MonadThrow
import cats.syntax.all.*
import dev.profunktor.auth.AuthHeaders
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F]) extends Http4sDsl[F]:
  private val httpRoutes: AuthedRoutes[AuthedUser, F] =
    AuthedRoutes.of {
      case ar @ POST -> Root / "logout" as user =>
        AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(user.id, _)) *> NoContent()
    }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(None, Some(authMiddleware(httpRoutes)))
