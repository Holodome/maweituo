package maweituo
package http
package endpoints.users

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class UserAdEndpoints[F[_]: MonadThrow](userAdsService: UserAdsService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getUserAdsEndpoint =
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

  override val endpoints = List(
    getUserAdsEndpoint
  ).map(_.tag("users"))
