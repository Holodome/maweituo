package com.holodome.domain

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.optics.uuidIso
import derevo.cats.eqv
import derevo.cats.show
import derevo.circe.magnolia.decoder
import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.circe.Json
import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID

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

  @derive(encoder)
  case class Message(
      sender: UserId,
      chat: ChatId,
      text: MessageText,
      at: Instant
  )

  @newtype case class HistoryResponse(messages: List[Message])

  object HistoryResponse {
    implicit val encoder: Encoder[HistoryResponse] = (a: HistoryResponse) =>
      Json.obj(
        ("messages", Json.fromValues(a.messages.map(Encoder[Message].apply)))
      )
  }

  @derive(decoder, show, eqv)
  case class SendMessageRequest(text: MessageText)
}
