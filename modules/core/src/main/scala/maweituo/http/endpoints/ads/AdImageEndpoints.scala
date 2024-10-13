package maweituo
package http
package endpoints.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.{MediaType as DomainMediaType, *}

import sttp.capabilities.fs2.Fs2Streams
import sttp.model.{HeaderNames, MediaType, StatusCode}
import sttp.tapir.*

final class AdImageEndpoints[F[_]: MonadThrow](imageService: AdImageService[F])(using builder: RoutesBuilder[F])
    extends Endpoints[F]:

  val getImageEndpoint =
    builder.public
      .get
      .in("ads" / path[AdId]("ad_id") / "imgs" / path[ImageId]("image_id"))
      .out(streamBinaryBody(Fs2Streams[F])(CodecFormat.OctetStream()))
      .out(header[String](HeaderNames.ContentType))
      .out(header[Long](HeaderNames.ContentLength))
      .serverLogic { (_, imageId) =>
        imageService
          .get(imageId)
          .map(x => (x.data, x.contentType.toRaw, x.dataSize))
          .toOut
      }

  val createImageEndpoint =
    builder.authed
      .post
      .in("ads" / path[AdId]("ad_id") / "imgs")
      .in(streamBinaryBody(Fs2Streams[F])(CodecFormat.OctetStream()))
      .in(header[MediaType](HeaderNames.ContentType))
      .in(header[Long](HeaderNames.ContentLength))
      .out(statusCode(StatusCode.Created))
      .serverLogic { authed => (adId, data, contentType, contentLength) =>
        given Identity = Identity(authed.id)
        val mediaType  = DomainMediaType.apply(contentType.mainType, contentType.subType)
        val contents   = ImageContentsStream(data, mediaType, contentLength)
        imageService.upload(adId, contents).void.toOut
      }

  val deleteImageEndpoint =
    builder.authed
      .delete
      .in("ads" / path[AdId]("ad_id") / "imgs" / path[ImageId]("image_id"))
      .out(statusCode(StatusCode.NoContent))
      .serverLogic { authed => (_, imageId) =>
        given Identity = Identity(authed.id)
        imageService.delete(imageId).toOut
      }

  override val endpoints = List(
    getImageEndpoint,
    createImageEndpoint,
    deleteImageEndpoint
  ).map(_.tag("ads"))
