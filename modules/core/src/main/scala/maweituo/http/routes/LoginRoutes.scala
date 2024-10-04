package maweituo.http.routes

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.users.LoginRequest
import maweituo.domain.users.services.AuthService
import maweituo.http.Routes
import maweituo.utils.given

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class LoginRoutes[F[_]: JsonDecoder: Concurrent](
    authService: AuthService[F]
) extends Http4sDsl[F]:

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        req.decode[LoginRequest] { login =>
          authService
            .login(login.name, login.password)
            .map { (jwt, _) => jwt }
            .flatMap(Ok(_))
        }
    }

  def routes: Routes[F] =
    Routes[F](Some(httpRoutes), None)
