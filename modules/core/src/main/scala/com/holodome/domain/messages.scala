package com.holodome.domain.messages

import java.time.Instant

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.utils.given
import com.holodome.utils.{IdNewtype, Newtype}

import cats.Show
import cats.derived.*
import io.circe.{ Codec, Decoder, Encoder, Json }

type ChatId = ChatId.Type
object ChatId extends IdNewtype

final case class Chat(
    id: ChatId,
    adId: AdId,
    adAuthor: UserId,
    client: UserId
) derives Codec.AsObject, Show

type MessageText = MessageText.Type
object MessageText extends Newtype[String]

final case class Message(
    sender: UserId,
    chat: ChatId,
    text: MessageText,
    at: Instant
) derives Codec.AsObject, Show

final case class HistoryResponse(messages: List[Message]) derives Show

object HistoryResponse:
  given Encoder[HistoryResponse] = (a: HistoryResponse) =>
    Json.obj(
      ("messages", Json.fromValues(a.messages.map(Encoder[Message].apply)))
    )

final case class SendMessageRequest(text: MessageText) derives Decoder, Show
