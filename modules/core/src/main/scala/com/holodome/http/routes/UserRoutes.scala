package com.holodome.http.routes

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.users._
import com.holodome.ext.http4s.refined.RefinedRequestDecoder
import com.holodome.http.vars.UserIdVar
import com.holodome.services.UserService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class UserRoutes[F[_]: MonadThrow: JsonDecoder](userService: UserService[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/users"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of { case GET -> Root / UserIdVar(userId) =>
    userService.find(userId).map(UserPublicInfo.fromUser).flatMap(Ok(_))
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
            .recoverWith { case InvalidAccess() =>
              Forbidden()
            }
        } else {
          BadRequest()
        }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): HttpRoutes[F] =
    Router(prefixPath -> (publicRoutes <+> authMiddleware(authedRoutes)))
}