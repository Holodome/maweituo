package maweituo
package http
package routes

import maweituo.domain.all.*

import dev.profunktor.auth.AuthHeaders
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final class LogoutRoutes[F[_]: JsonDecoder: MonadThrow](authService: AuthService[F])
    extends Http4sDsl[F] with UserAuthRoutes[F]:

  override val routes: AuthedRoutes[AuthedUser, F] =
    AuthedRoutes.of {
      case ar @ POST -> Root / "logout" as user =>
        given Identity = Identity(user.id)
        AuthHeaders.getBearerToken(ar.req).traverse_(authService.logout(_)) *> NoContent()
    }
