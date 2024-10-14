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

trait AdEndpointDefs(using builder: EndpointBuilderDefs):

  val `get /ads/$adId` =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id"))
      .out(jsonBody[AdResponseDto])

  val `post /ads` =
    builder.authed
      .post
      .in("ads")
      .in(jsonBody[CreateAdRequestDto])
      .out(jsonBody[CreateAdResponseDto])
      .out(statusCode(StatusCode.Created))

  val `delete /ads/$adId` =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id"))
      .out(statusCode(StatusCode.NoContent))

  val `put /ads/$adId` =
    builder.authed
      .put
      .in("ads" / path[AdId]("ad_id"))
      .in(jsonBody[UpdateAdRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class AdEndpoints[F[_]: MonadThrow](adService: AdService[F])(using EndpointsBuilder[F])
    extends AdEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    `get /ads/$adId`.serverLogicF { adId =>
      adService
        .get(adId)
        .map(AdResponseDto.fromDomain)
    },
    `post /ads`.authedServerLogic { create =>
      adService
        .create(create.toDomain)
        .map(CreateAdResponseDto.apply)
    },
    `delete /ads/$adId`.authedServerLogic { adId =>
      adService.delete(adId)
    },
    `put /ads/$adId`.authedServerLogic { (adId, req) =>
      adService.update(req.toDomain(adId))
    }
  ).map(_.tag("ads"))
