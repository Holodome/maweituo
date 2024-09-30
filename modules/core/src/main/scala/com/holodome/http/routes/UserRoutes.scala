package com.holodome.http.routes

import com.holodome.domain.services.{ UserAdsService, UserService }
import com.holodome.domain.users.*
import com.holodome.http.Routes
import com.holodome.http.vars.UserIdVar

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.typelevel.log4cats.Logger

final case class UserRoutes[F[_]: JsonDecoder: Logger: Concurrent](
    userService: UserService[F],
    userAdsService: UserAdsService[F]
) extends Http4sDsl[F]:
  private val prefixPath = "/users"

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / UserIdVar(userId) =>
      userService.get(userId).map(UserPublicInfo.fromUser).flatMap(Ok(_))

    case GET -> Root / UserIdVar(userId) / "ads" =>
      userAdsService.getAds(userId).flatMap(Ok(_))
  }

  private val authedRoutes: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case DELETE -> Root / UserIdVar(userId) as user =>
      userService.delete(userId, user.id) *> NoContent()
    case ar @ PUT -> Root / UserIdVar(userId) as user =>
      ar.req.decode[UpdateUserRequest] { update =>
        // This check is here only because we are restful
        if userId === update.id then
          userService
            .update(update, user.id)
            .flatMap(Ok(_))
        else
          BadRequest()
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AuthedUser]): Routes[F] =
    Routes(
      Some(Router(prefixPath -> publicRoutes)),
      Some(Router(prefixPath -> authMiddleware(authedRoutes)))
    )
