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

class TagEndpointDefs(using builder: EndpointBuilderDefs):

  val getAllTagsEndpoint =
    builder.public
      .get
      .in("tags")
      .out(jsonBody[AllTagsResponse])

  val getTagAdsEndpoint =
    builder.public
      .get
      .in("tags" / path[AdTag]("tag") / "ads")
      .out(jsonBody[TagAdsResponse])

final class TagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using EndpointsBuilder[F])
    extends TagEndpointDefs with Endpoints[F]:

  override val endpoints = List(
    getAllTagsEndpoint.serverLogic { _ =>
      tags.all.map(AllTagsResponse.apply).toOut
    }.tag("ads"),
    getTagAdsEndpoint.serverLogic { tag =>
      tags.find(tag).map(TagAdsResponse(tag, _)).toOut
    }.tag("ads")
  )
