package com.holodome.domain

import com.holodome.domain.users.UserId
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

import java.util.UUID

object advertisements {
  @derive(decoder, encoder)
  @newtype case class AdvertisementId(value: UUID)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTitle(value: String)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTag(value: String)

  case class Advertisement(
      id: AdvertisementId,
      title: AdvertisementTitle,
      tags: List[AdvertisementTag],
      authorId: UserId
  )
}
