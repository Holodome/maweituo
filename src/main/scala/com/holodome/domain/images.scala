package com.holodome.domain

import cats.syntax.all._
import com.holodome.domain.advertisements.AdId
import com.holodome.optics.uuid
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.{Base64, UUID}
import scala.util.Try

object images {
  @derive(uuid, encoder, decoder, show)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder)
  @newtype case class ImageUrl(value: String)

  @newtype case class ImageContents(value: Array[Byte])

  object ImageContents {
    implicit val jsonDecoder: Decoder[ImageContents] =
      Decoder.decodeString.emapTry(str =>
        Try(Base64.getDecoder.decode(str)).map(ImageContents.apply)
      )
    implicit val jsonEncoder: Encoder[ImageContents] =
      Encoder.encodeString.contramap[ImageContents](img =>
        Base64.getEncoder.encodeToString(img.value)
      )
  }

  @derive(encoder)
  case class Image(
      id: ImageId,
      adId: AdId,
      url: ImageUrl
  )
}
