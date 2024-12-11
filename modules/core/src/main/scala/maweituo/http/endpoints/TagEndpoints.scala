package maweituo
package http
package endpoints

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

trait TagEndpointDefs(using builder: EndpointBuilderDefs):

  def `get /tags` =
    builder.public
      .get
      .in("tags")
      .out(jsonBody[AllTagsResponseDto])

  def `get /tags/$tag/ads` =
    builder.public
      .get
      .in("tags" / path[AdTag]("tag") / "ads")
      .out(jsonBody[TagAdsResponseDto])

final class TagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using EndpointsBuilder[F])
    extends TagEndpointDefs with Endpoints[F]:

  override def endpoints = List(
    `get /tags`.serverLogicF { _ =>
      tags.all.map(AllTagsResponseDto.apply)
    }.tag("ads"),
    `get /tags/$tag/ads`.serverLogicF { tag =>
      tags.find(tag).map(TagAdsResponseDto(tag, _))
    }.tag("ads")
  )
