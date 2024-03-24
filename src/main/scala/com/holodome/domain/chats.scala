package com.holodome.domain

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.users.UserId
import derevo.derive
import com.holodome.optics.uuid
import io.estatico.newtype.macros.newtype

import java.util.UUID

object chats {
  @derive(uuid)
  @newtype case class ChatId(id: UUID)

  case class Chat(
      adId: AdvertisementId,
      adAuthor: UserId,
      client: UserId
  )
}
