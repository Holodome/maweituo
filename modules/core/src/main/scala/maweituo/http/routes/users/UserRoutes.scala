package maweituo
package http
package routes
package users

import cats.effect.Concurrent

import maweituo.domain.all.*

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger

final class UserRoutes[F[_]: JsonDecoder: Logger: Concurrent](
    userService: UserService[F]
) extends Http4sDsl[F] with BothRoutes[F]:

  override val publicRoutes = HttpRoutes.of {
    case GET -> Root / "users" / UserIdVar(userId) =>
      userService
        .get(userId)
        .map(UserPublicInfoDto.fromUser)
        .flatMap(Ok(_))
  }

  override val authRoutes = AuthedRoutes.of {
    case DELETE -> Root / "users" / UserIdVar(userId) as user =>
      given Identity = Identity(user.id)
      userService.delete(userId) *> NoContent()

    case ar @ PUT -> Root / "users" / UserIdVar(userId) as user =>
      given Identity = Identity(user.id)
      ar.req.decode[UpdateUserRequestDto] { update =>
        userService.update(update.toDomain) *> NoContent()
      }
  }
