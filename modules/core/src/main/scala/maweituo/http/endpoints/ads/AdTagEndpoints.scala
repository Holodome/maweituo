package maweituo
package http
package endpoints
package ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdTagEndpointDefs(using builder: EndpointBuilderDefs):

  def `get /ads/$adId/tags` =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "tags")
      .out(jsonBody[AdTagsResponseDto])

  def `post /ads/$adId/tags` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.Created))

  def `delete /ads/$adId/tags` =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.NoContent))

final class AdTagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using EndpointsBuilder[F])
    extends AdTagEndpointDefs with Endpoints[F]:

  override def endpoints = List(
    `get /ads/$adId/tags`.serverLogicF { adId =>
      tags
        .adTags(adId)
        .map(AdTagsResponseDto(adId, _))
    },
    `post /ads/$adId/tags`.authedServerLogic { (adId, req) =>
      tags.addTag(adId, req.tag)
    },
    `delete /ads/$adId/tags`.authedServerLogic { (adId, req) =>
      tags.removeTag(adId, req.tag)
    }
  ).map(_.tag("ads"))
