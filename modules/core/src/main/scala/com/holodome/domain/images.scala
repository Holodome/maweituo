package com.holodome.domain

import cats.syntax.all._
import cats.{Applicative, MonadThrow, Show}
import cats.data.{EitherT, OptionT}
import cats.effect.kernel.Concurrent
import com.holodome.domain.ads.AdId
import com.holodome.optics.uuidIso
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.http4s.{
  AuthedRoutes,
  Entity,
  EntityDecoder,
  EntityEncoder,
  Header,
  Headers,
  HttpRoutes,
  MalformedMessageBodyFailure,
  Media,
  MediaRange,
  MediaType
}

import java.util.UUID

object images {
  @derive(uuidIso, encoder, decoder, show, eqv)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder, eqv)
  @newtype case class ImageUrl(value: String)

  case class ImageContents(data: Array[Byte], contentType: MediaType)

  object ImageContents {
    implicit val show: Show[ImageContents] = Show.show(_ => "ImageContents")

    implicit def imageDecoder[F[_]: MonadThrow: Concurrent]: EntityDecoder[F, ImageContents] =
      EntityDecoder.decodeBy(MediaRange.`image/*`) { (m: Media[F]) =>
        EitherT.liftF(
          (
            m.as[Array[Byte]],
            OptionT
              .fromOption(m.contentType)
              .getOrRaise(MalformedMessageBodyFailure("Expected Content-Type header"))
          ).tupled.map { case (arr, contentType) =>
            ImageContents(
              arr,
              MediaType(contentType.mediaType.mainType, contentType.mediaType.subType)
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
      mediaType: MediaType
  )
}
