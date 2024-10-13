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

class UserAdEndpointDefs(using builder: EndpointBuilderDefs):

  val getUserAdsEndpoint =
    builder.public
      .get
      .in("users" / path[UserId]("user_id") / "ads")
      .out(jsonBody[UserAdsResponseDto])

final class UserAdEndpoints[F[_]: MonadThrow](userAdsService: UserAdsService[F])(using EndpointsBuilder[F])
    extends UserAdEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    getUserAdsEndpoint.serverLogic { userId =>
      userAdsService
        .getAds(userId)
        .map(UserAdsResponseDto(userId, _))
        .toOut
    }
  ).map(_.tag("users"))
