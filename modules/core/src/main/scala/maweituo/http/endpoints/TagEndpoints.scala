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

final class TagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getAllTagsEndpoint =
    builder.public
      .get
      .in("tags")
      .out(jsonBody[AllTagsResponse])
      .serverLogic { _ =>
        tags.all.map(AllTagsResponse.apply).toOut
      }

  val getTagAdsEndpoint =
    builder.public
      .get
      .in("tags" / path[AdTag]("tag") / "ads")
      .out(jsonBody[TagAdsResponse])
      .serverLogic { tag =>
        tags.find(tag).map(TagAdsResponse(tag, _)).toOut
      }

  override val endpoints = List(
    getAllTagsEndpoint,
    getTagAdsEndpoint
  ).map(_.tag("ads"))
