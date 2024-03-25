package com.holodome.domain

import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.queryParam
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.Try
import scala.util.control.NoStackTrace
import com.holodome.optics.uuid

object advertisements {
  @derive(decoder, encoder, uuid)
  @newtype case class AdvertisementId(value: UUID)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTitle(value: String)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTag(value: String)

  @derive(encoder)
  case class Advertisement(
      id: AdvertisementId,
      title: AdvertisementTitle,
      tags: List[AdvertisementTag],
      chats: List[ChatId],
      authorId: UserId
  )

  @derive(decoder, encoder)
  case class CreateAdRequest(
      title: AdvertisementTitle
  )

  @derive(queryParam)
  @newtype case class AdvertisementParam(value: String) {
    def toDomain: Option[AdvertisementId] =
      Try(UUID.fromString(value)).map(AdvertisementId.apply).toOption
  }

  object AdvertisementParam {
    implicit val jsonEncoder: Encoder[AdvertisementParam] =
      Encoder.forProduct1("id")(_.value)
    implicit val jsonDecoder: Decoder[AdvertisementParam] =
      Decoder.forProduct1("id")(AdvertisementParam.apply)
  }

  final case class InvalidAdId(id: AdvertisementId) extends NoStackTrace
  final case class CannotCreateChatWithMyself() extends NoStackTrace
  final case class ChatAlreadyExists() extends NoStackTrace
  final case class NotAnAuthor() extends NoStackTrace
}
