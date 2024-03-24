package com.holodome.domain

import derevo.circe.magnolia.{decoder, encoder}
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.users.UserId
import derevo.derive
import io.estatico.newtype.macros.newtype
import com.holodome.optics.uuid

import java.time.Instant
import java.util.UUID
import scala.util.control.NoStackTrace

object messages {
  @derive(uuid, encoder, decoder)
  @newtype case class ChatId(id: UUID)

  case class Chat(
      adId: AdvertisementId,
      adAuthor: UserId,
      client: UserId
  )

  @derive(uuid)
  @newtype case class MessageId(value: UUID)
  @newtype case class MessageText(value: String)

  case class ChatNotFound(adId: AdvertisementId) extends NoStackTrace

  case class Message(
      id: MessageId,
      from: UserId,
      to: UserId,
      ad: AdvertisementId,
      text: MessageText,
      at: Instant
  )
}
