package maweituo
package http
package routes

import cats.effect.Concurrent

import maweituo.domain.all.*

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl

final class RegisterRoutes[F[_]: Concurrent: JsonDecoder](userService: UserService[F])
    extends Http4sDsl[F] with PublicRoutes[F]:

  override val routes: HttpRoutes[F] =
    HttpRoutes.of {
      case req @ POST -> Root / "register" =>
        req.decode[RegisterRequestDto] { register =>
          userService
            .create(register.toDomain)
            .map(RegisterResponseDto.apply)
            .flatMap(Ok(_))
        }
    }
