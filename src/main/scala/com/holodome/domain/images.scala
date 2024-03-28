package com.holodome.domain

import cats.Show
import com.holodome.domain.ads.AdId
import com.holodome.optics.{cassandraReads, uuidIso}
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.{Base64, UUID}
import scala.util.Try
import scala.util.control.NoStackTrace

object images {
  @derive(uuidIso, encoder, decoder, show, cassandraReads)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder, cassandraReads)
  @newtype case class ImageUrl(value: String)

  @newtype case class ImageContents(value: Array[Byte])

  object ImageContents {
    implicit val show: Show[ImageContents] = Show.show(_ => "ImageContents")

    implicit val jsonDecoder: Decoder[ImageContents] =
      Decoder.decodeString.emapTry(str =>
        Try(Base64.getDecoder.decode(str)).map(ImageContents.apply)
      )
    implicit val jsonEncoder: Encoder[ImageContents] =
      Encoder.encodeString.contramap[ImageContents](img =>
        Base64.getEncoder.encodeToString(img.value)
      )
  }

  case class InvalidImageId()      extends NoStackTrace
  case class InternalImageUnsync() extends NoStackTrace

  @derive(encoder)
  case class Image(
      id: ImageId,
      adId: AdId,
      url: ImageUrl
  )
}
