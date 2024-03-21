package com.holodome.domain

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.users.UserId
import io.estatico.newtype.macros.newtype

object messages {
  @newtype case class MessageText(value: String)

  case class Message(from: UserId, to: UserId, ad: AdvertisementId, text: MessageText)
}
