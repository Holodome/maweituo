package maweituo.http.routes

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.users.AuthedUser
import maweituo.domain.users.services.AuthService
import maweituo.http.UserAuthRoutes

import dev.profunktor.auth.AuthHeaders
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] =
    AuthedRoutes.of {
      case ar @ POST -> Root / "logout" as user =>
        AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(user.id, _)) *> NoContent()
    }
