package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.errors.ApplicationError
import com.holodome.domain.users._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.UserIdVar
import com.holodome.http.{HttpErrorHandler, Routes}
import com.holodome.services.UserService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final case class UserRoutes[F[_]: MonadThrow: JsonDecoder: Logger](userService: UserService[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/users"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of { case GET -> Root / UserIdVar(userId) =>
    Logger[F].info(s"hello world") *>
      userService.get(userId).map(UserPublicInfo.fromUser).flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case DELETE -> Root / UserIdVar(userId) as user =>
      userService.delete(userId, user.id) *> NoContent()
    case ar @ PUT -> Root / UserIdVar(userId) as user =>
      ar.req.decodeR[UpdateUserRequest] { update =>
        // This check is here only because we are restful
        if (userId === update.id) {
          userService
            .update(update, user.id)
            .flatMap(Ok(_))
        } else {
          BadRequest()
        }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser])(implicit
      H: HttpErrorHandler[F, ApplicationError]
  ): Routes[F] =
    Routes(
      Some(Router(prefixPath -> H.handle(publicRoutes))),
      Some(Router(prefixPath -> H.handle(authMiddleware(authedRoutes))))
    )
}
