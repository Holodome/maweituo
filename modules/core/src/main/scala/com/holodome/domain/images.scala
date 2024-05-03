package com.holodome.domain

import cats.{MonadThrow, Show}
import cats.data.{EitherT, OptionT}
import cats.effect.Concurrent
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.optics.uuidIso
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.http4s.{EntityDecoder, MalformedMessageBodyFailure, Media, MediaRange}

import java.util.UUID

object images {
  @derive(uuidIso, encoder, decoder, show, eqv)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder, eqv)
  @newtype case class ImageUrl(value: String)

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
