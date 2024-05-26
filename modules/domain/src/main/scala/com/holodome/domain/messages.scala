package com.holodome.domain

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.optics.uuidIso
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.circe.{Encoder, Json}
import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID
import cats.Functor
import com.holodome.utils.EncodeRF

package object messages {
  @derive(uuidIso, encoder, decoder, eqv)
  @newtype case class ChatId(value: UUID)

  @derive(encoder)
  case class Chat(
      id: ChatId,
      adId: AdId,
      adAuthor: UserId,
      client: UserId
  )

  @derive(encoder, decoder, show, eqv)
  @newtype case class MessageText(_value: NonEmptyString) {
    def value: String = _value.value
  }
  implicit def messageTextEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, MessageText] = EncodeRF.map(MessageText.apply)


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
