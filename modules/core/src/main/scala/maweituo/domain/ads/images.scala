package maweituo.domain.ads.images

import cats.data.{EitherT, OptionT}
import cats.derived.*
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.{MonadThrow, Show}

import maweituo.domain.ads.AdId
import maweituo.infrastructure.OBSId
import maweituo.utils.{IdNewtype, Newtype}

import org.http4s.*

type ImageId = ImageId.Type
object ImageId extends IdNewtype

type ImageUrl = ImageUrl.Type
object ImageUrl extends Newtype[String]:
  given Conversion[ImageUrl, OBSId] with
    def apply(x: ImageUrl): OBSId = OBSId(x.value)

final case class ImageContentsStream[+F[_]](
    data: fs2.Stream[F, Byte],
    contentType: MediaType,
    dataSize: Long
)

object ImageContentsStream:
  given [F[_]]: Show[ImageContentsStream[F]] = Show.show(_ => "ImageContents")

  given [F[_]: MonadThrow: Concurrent]: EntityDecoder[F, ImageContentsStream[F]] =
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

final case class MediaType(mainType: String, subType: String) derives Show:
  def toRaw: String = s"$mainType/$subType"

object MediaType:
  def fromRaw(raw: String): Option[MediaType] =
    raw.split("/") match
      case Array(a, b) => MediaType(a, b).some
      case _           => None

final case class Image(
    id: ImageId,
    adId: AdId,
    url: ImageUrl,
    mediaType: MediaType,
    size: Long
) derives Show