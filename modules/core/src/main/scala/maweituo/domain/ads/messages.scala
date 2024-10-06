package maweituo.domain.ads.messages

import java.time.Instant

import cats.Show
import cats.derived.*

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId
import maweituo.utils.given
import maweituo.utils.{IdNewtype, Newtype}

type ChatId = ChatId.Type
object ChatId extends IdNewtype

final case class Chat(
    id: ChatId,
    adId: AdId,
    adAuthor: UserId,
    client: UserId
) derives Show

type MessageText = MessageText.Type
object MessageText extends Newtype[String]

final case class Message(
    sender: UserId,
    chat: ChatId,
    text: MessageText,
    at: Instant
) derives Show

final case class HistoryResponse(messages: List[Message]) derives Show

final case class SendMessageRequest(text: MessageText) derives Show
