package maweituo.http.routes.users

import cats.effect.Concurrent
import cats.syntax.all.*

import maweituo.domain.users.*
import maweituo.domain.users.services.UserService
import maweituo.http.BothRoutes
import maweituo.http.vars.UserIdVar

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger
import maweituo.http.dto.UpdateUserRequestDto
import maweituo.http.dto.UserPublicInfoDto

final case class UserRoutes[F[_]: JsonDecoder: Logger: Concurrent](
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
      userService.delete(userId, user.id) *> NoContent()

    case ar @ PUT -> Root / "users" / UserIdVar(userId) as user =>
      ar.req.decode[UpdateUserRequestDto] { update =>
        // This check is here only because we are restful
        if userId === update.id then
          userService.update(update.toDomain, user.id) *> NoContent()
        else
          BadRequest()
      }
  }
