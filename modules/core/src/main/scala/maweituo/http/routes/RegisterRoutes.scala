package maweituo.http.routes

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.users.RegisterRequest
import maweituo.domain.users.services.UserService
import maweituo.http.PublicRoutes

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final case class RegisterRoutes[F[_]: Concurrent: JsonDecoder](userService: UserService[F])
    extends Http4sDsl[F] with PublicRoutes[F]:

  override val routes: HttpRoutes[F] =
    HttpRoutes.of {
      case req @ POST -> Root / "register" =>
        req.decode[RegisterRequest] { register =>
          userService
            .create(register)
            .flatMap(Ok(_))
        }
    }
