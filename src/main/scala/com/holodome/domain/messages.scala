package com.holodome.domain

import com.holodome.domain.advertisements.AdId
import com.holodome.domain.users.UserId
import com.holodome.optics.uuid
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Encoder, Json}
import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID
import scala.util.control.NoStackTrace

object messages {
  @derive(uuid, encoder, decoder)
  @newtype case class ChatId(id: UUID)

  case class Chat(
      adId: AdId,
      adAuthor: UserId,
      client: UserId
  )

  @derive(uuid, encoder, decoder)
  @newtype case class MessageId(value: UUID)
  @derive(encoder, decoder)
  @newtype case class MessageText(value: String)

  case class InvalidChatId()       extends NoStackTrace
  case class ChatAccessForbidden() extends NoStackTrace

  @derive(encoder)
  case class Message(
      id: MessageId,
      sender: UserId,
      ad: AdId,
      text: MessageText,
      at: Instant
  )

  @newtype
  case class HistoryResponse(messages: List[Message])

  object HistoryResponse {
    implicit val encoder: Encoder[HistoryResponse] = (a: HistoryResponse) =>
      Json.obj(
        ("messages", Json.fromValues(a.messages.map(Encoder[Message].apply(_))))
      )
  }

  @derive(decoder)
  case class SendMessageRequest(text: MessageText)
}
