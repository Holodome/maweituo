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
import org.http4s.{EntityDecoder, MalformedMessageBodyFailure, Media, MediaRange}
import org.http4s.{
  AuthedRoutes,
  EntityDecoder,
  Header,
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

  case class ImageContents(data: Array[Byte], contentType: String)

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
              s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
            )
          }
        )
      }
  }

  @derive(encoder)
  case class Image(
      id: ImageId,
      adId: AdId,
      url: ImageUrl,
      mediaType: String
  )
}
