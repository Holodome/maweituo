package maweituo.http.routes

import maweituo.domain.users.RegisterRequest
import maweituo.domain.users.services.UserService
import maweituo.http.Routes

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class RegisterRoutes[F[_]: Concurrent: JsonDecoder](userService: UserService[F]) extends Http4sDsl[F]:

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of {
      case req @ POST -> Root / "register" =>
        req.decode[RegisterRequest] { register =>
          userService
            .create(register)
            .flatMap(Ok(_))
        }
    }

  def routes: Routes[F] =
    Routes[F](Some(httpRoutes), None)
