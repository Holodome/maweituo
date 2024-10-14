package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.{MediaType as DomainMediaType, *}

import sttp.capabilities.fs2.Fs2Streams
import sttp.model.{HeaderNames, MediaType, StatusCode}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

trait AdImageEndpointDefs[F[_]](using builder: EndpointBuilderDefs):

  val `get /ads/$adId/imgs/$imgId` =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "imgs" / path[ImageId]("image_id"))
      .out(streamBinaryBody(Fs2Streams[F])(CodecFormat.OctetStream()))
      .out(header[String](HeaderNames.ContentType))
      .out(header[Long](HeaderNames.ContentLength))

  val `post /ads/$adId/imgs` =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "imgs")
      .in(streamBinaryBody(Fs2Streams[F])(CodecFormat.OctetStream()))
      .in(header[MediaType](HeaderNames.ContentType))
      .in(header[Long](HeaderNames.ContentLength))
      .out(jsonBody[CreateImageRequestDto])
      .out(statusCode(StatusCode.Created))

  val `delete /ads/$adId/imgs/$imgId` =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "imgs" / path[ImageId]("image_id"))
      .out(statusCode(StatusCode.NoContent))

final class AdImageEndpoints[F[_]: MonadThrow](imageService: AdImageService[F])(using EndpointsBuilder[F])
    extends AdImageEndpointDefs[F] with Endpoints[F]:

  override val endpoints = List(
    `get /ads/$adId/imgs/$imgId`.serverLogicF { (_, imageId) =>
      imageService
        .get(imageId)
        .map(x => (x.data, x.contentType.toRaw, x.dataSize))
    },
    `post /ads/$adId/imgs`.authedServerLogic { (adId, data, contentType, contentLength) =>
      val mediaType = DomainMediaType.apply(contentType.mainType, contentType.subType)
      val contents  = ImageContentsStream(data, mediaType, contentLength)
      imageService.upload(adId, contents).map(CreateImageRequestDto.apply)
    },
    `delete /ads/$adId/imgs/$imgId`.authedServerLogic { (_, imageId) =>
      imageService.delete(imageId)
    }
  ).map(_.tag("ads"))
