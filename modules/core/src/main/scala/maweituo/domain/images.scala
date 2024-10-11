package maweituo
package domain

import maweituo.infrastructure.OBSId
import maweituo.utils.{IdNewtype, Newtype}

object images:
  import maweituo.domain.ads.*

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
