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

final class AdTagEndpoints[F[_]: MonadThrow](tags: AdTagService[F], builder: RoutesBuilder[F])
    extends Endpoints[F]:

  override val endpoints = List(
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "tags")
      .out(jsonBody[AdTagsResponseDto])
      .serverLogic { adId =>
        tags
          .adTags(adId)
          .map(AdTagsResponseDto(adId, _))
          .toOut
      },
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => (adId, req) =>
        given Identity = Identity(authed.id)
        tags.addTag(adId, req.tag).toOut
      },
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => (adId, req) =>
        given Identity = Identity(authed.id)
        tags.removeTag(adId, req.tag).toOut
      }
  )
