package maweituo
package http
package routes
package ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final class AdRoutes[F[_]: MonadThrow](adService: AdService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id"))
      .out(jsonBody[AdResponseDto])
      .serverLogic { adId =>
        adService
          .get(adId)
          .map(AdResponseDto.fromDomain)
          .toOut
      },
    builder.authed
      .post
      .in("ads")
      .in(jsonBody[CreateAdRequestDto])
      .out(jsonBody[CreateAdResponseDto])
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => create =>
        given Identity = Identity(authed.id)
        adService
          .create(create.toDomain)
          .map(CreateAdResponseDto.apply)
          .toOut
      },
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id"))
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => adId =>
        given Identity = Identity(authed.id)
        adService
          .delete(adId)
          .toOut
      },
    builder.authed
      .put
      .in("ads" / path[AdId]("ad_id"))
      .in(jsonBody[UpdateAdRequestDto])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => (adId, req) =>
        given Identity = Identity(authed.id)
        adService
          .update(req.toDomain(adId))
          .toOut
      }
  )
