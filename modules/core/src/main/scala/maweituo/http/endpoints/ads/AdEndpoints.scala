package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

class AdEndpointDefs(using builder: EndpointBuilderDefs):

  val getAdEndpoint =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id"))
      .out(jsonBody[AdResponseDto])

  val createAdEndpoint =
    builder.authed
      .post
      .in("ads")
      .in(jsonBody[CreateAdRequestDto])
      .out(jsonBody[CreateAdResponseDto])
      .out(statusCode(StatusCode.Created))

  val deleteAdEndpoint =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id"))
      .out(statusCode(StatusCode.NoContent))

  val updateAdEndpoint =
    builder.authed
      .put
      .in("ads" / path[AdId]("ad_id"))
      .in(jsonBody[UpdateAdRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class AdEndpoints[F[_]: MonadThrow](adService: AdService[F])(using EndpointsBuilder[F])
    extends AdEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    getAdEndpoint.serverLogic { adId =>
      adService
        .get(adId)
        .map(AdResponseDto.fromDomain)
        .toOut
    },
    createAdEndpoint.secure.serverLogic { authed => create =>
      given Identity = Identity(authed.id)
      adService
        .create(create.toDomain)
        .map(CreateAdResponseDto.apply)
        .toOut
    },
    deleteAdEndpoint.secure.serverLogic { authed => adId =>
      given Identity = Identity(authed.id)
      adService
        .delete(adId)
        .toOut
    },
    updateAdEndpoint.secure.serverLogic { authed => (adId, req) =>
      given Identity = Identity(authed.id)
      adService
        .update(req.toDomain(adId))
        .toOut
    }
  ).map(_.tag("ads"))
