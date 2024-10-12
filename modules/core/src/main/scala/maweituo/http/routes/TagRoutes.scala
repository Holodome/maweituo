package maweituo
package http
package routes

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

final class TagRoutes[F[_]: MonadThrow](tags: AdTagService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
    builder.public
      .get
      .in("tags")
      .out(jsonBody[AllTagsResponse])
      .serverLogic { _ =>
        tags.all.map(AllTagsResponse.apply).toOut
      },
    builder.public
      .get
      .in("tags" / path[AdTag]("tag") / "ads")
      .out(jsonBody[TagAdsResponse])
      .serverLogic { tag =>
        tags.find(tag).map(TagAdsResponse(tag, _)).toOut
      }
  )
