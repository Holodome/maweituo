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

final class AdTagEndpoints[F[_]: MonadThrow](tags: AdTagService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getAdTagsEndpoint =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "tags")
      .out(jsonBody[AdTagsResponseDto])
      .serverLogic { adId =>
        tags
          .adTags(adId)
          .map(AdTagsResponseDto(adId, _))
          .toOut
      }

  val addAdTagEndpoint =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => (adId, req) =>
        given Identity = Identity(authed.id)
        tags.addTag(adId, req.tag).toOut
      }

  val deleteAdTagEndpoint =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "tags")
      .in(jsonBody[AddTagRequestDto])
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => (adId, req) =>
        given Identity = Identity(authed.id)
        tags.removeTag(adId, req.tag).toOut
      }

  override val endpoints = List(
    getAdTagsEndpoint,
    addAdTagEndpoint,
    deleteAdTagEndpoint
  ).map(_.tag("ads"))
