package com.holodome.domain

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.optics.uuid
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

import java.util.UUID

object images {
  @derive(uuid, encoder, decoder)
  @newtype case class ImageId(id: UUID)

  @derive(encoder, decoder)
  @newtype case class ImageUrl(value: String)

  @newtype case class ImageContents(value: Array[Byte])

  @derive(encoder)
  case class Image(
      id: ImageId,
      adId: AdvertisementId,
      url: ImageUrl
  )
}
