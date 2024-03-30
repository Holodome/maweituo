package com.holodome.domain

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.optics.uuidIso
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Encoder, Json}
import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID
import scala.util.control.NoStackTrace

object messages {
  @derive(uuidIso, encoder, decoder, eqv)
  @newtype case class ChatId(id: UUID)

  case class Chat(
      id: ChatId,
      adId: AdId,
      adAuthor: UserId,
      client: UserId
  )

  @derive(encoder, decoder, show, eqv)
  @newtype case class MessageText(value: String)

  case class InvalidChatId()       extends NoStackTrace
  case class ChatAccessForbidden() extends NoStackTrace

  @derive(encoder)
  case class Message(
      sender: UserId,
      chat: ChatId,
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

  @derive(decoder, show, eqv)
  case class SendMessageRequest(text: MessageText)
}
