package maweituo
package http
package routes
package users

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class UserAdRoutes[F[_]: MonadThrow](
    userAdsService: UserAdsService[F],
    builder: RoutesBuilder[F]
) extends Endpoints[F]:

  override val endpoints = List(
    builder.public
      .get
      .in("users" / path[UserId]("user_id") / "ads")
      .out(jsonBody[UserAdsResponseDto])
      .serverLogic { userId =>
        userAdsService
          .getAds(userId)
          .map(UserAdsResponseDto(userId, _))
          .toOut
      }
  )
