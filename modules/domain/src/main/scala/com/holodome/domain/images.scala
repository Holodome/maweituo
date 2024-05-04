package com.holodome.domain

import cats.MonadThrow
import cats.Show
import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Concurrent
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.infrastructure.ObjectStorage.OBSId
import com.holodome.optics.uuidIso
import derevo.cats.eqv
import derevo.cats.show
import derevo.circe.magnolia.decoder
import derevo.circe.magnolia.encoder
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.http4s.EntityDecoder
import org.http4s.MalformedMessageBodyFailure
import org.http4s.Media
import org.http4s.MediaRange

import java.util.UUID

object images {
  @derive(uuidIso, encoder, decoder, show, eqv)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder, eqv)
  @newtype case class ImageUrl(value: String) {
    def toObsID: OBSId = OBSId(value)
  }

  object ImageUrl {
    def fromObsId(id: OBSId): ImageUrl =
      ImageUrl(id.value)
  }

  implicit class ImageUrlOBSConv(id: OBSId) {
    def toImageUrl: ImageUrl = ImageUrl.fromObsId(id)
  }

  case class ImageContentsStream[+F[_]](
      data: fs2.Stream[F, Byte],
      contentType: MediaType,
      dataSize: Long
  )

  object ImageContentsStream {
    implicit def show[F[_]]: Show[ImageContentsStream[F]] = Show.show(_ => "ImageContents")

    implicit def imageDecoder[F[_]: MonadThrow: Concurrent]
        : EntityDecoder[F, ImageContentsStream[F]] =
      EntityDecoder.decodeBy(MediaRange.`image/*`) { (m: Media[F]) =>
        EitherT.liftF(
          (
            OptionT
              .fromOption(m.contentType)
              .getOrRaise(MalformedMessageBodyFailure("Expected Content-Type header")),
            OptionT
              .fromOption(m.contentLength)
              .getOrRaise(MalformedMessageBodyFailure("Expected Content-Length header"))
          ).tupled
            .map { case (contentType, contentLength) =>
              ImageContentsStream(
                m.body,
                MediaType(contentType.mediaType.mainType, contentType.mediaType.subType),
                contentLength
              )
            }
        )
      }
  }

  case class MediaType(mainType: String, subType: String) {
    def toRaw = s"$mainType/$subType"
  }
  object MediaType {
    def fromRaw(raw: String): Option[MediaType] = {
      raw.split("/") match {
        case Array(a, b) => MediaType(a, b).some
        case _           => None
      }
    }
  }

  case class Image(
      id: ImageId,
      adId: AdId,
      url: ImageUrl,
      mediaType: MediaType,
      size: Long
  )
}
