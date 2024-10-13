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

class AdTagEndpointDefs(using builder: EndpointBuilderDefs):

  val getAdTagsEndpoint =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "tags")
      .out(jsonBody[AdTagsResponseDto])

  val addAdTagEndpoint =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.Created))

  val deleteAdTagEndpoint =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class AdTagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using EndpointsBuilder[F])
    extends AdTagEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    getAdTagsEndpoint.serverLogic { adId =>
      tags
        .adTags(adId)
        .map(AdTagsResponseDto(adId, _))
        .toOut
    },
    addAdTagEndpoint.secure.serverLogic { authed => (adId, req) =>
      given Identity = Identity(authed.id)
      tags.addTag(adId, req.tag).toOut
    },
    deleteAdTagEndpoint.secure.serverLogic { authed => (adId, req) =>
      given Identity = Identity(authed.id)
      tags.removeTag(adId, req.tag).toOut
    }
  ).map(_.tag("ads"))
