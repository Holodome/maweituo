package com.holodome.domain

import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.queryParam
import com.holodome.optics.uuid
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.Try
import scala.util.control.NoStackTrace

object advertisements {
  @derive(decoder, encoder, uuid, eqv)
  @newtype case class AdId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class AdTitle(value: String)

  @derive(decoder, encoder, eqv)
  @newtype case class AdTag(value: String)

  @derive(encoder)
  case class Advertisement(
      id: AdId,
      title: AdTitle,
      tags: List[AdTag],
      images: List[ImageId],
      chats: List[ChatId],
      authorId: UserId
  )

  @derive(decoder, encoder, show)
  case class CreateAdRequest(
      title: AdTitle
  )

  @derive(queryParam)
  @newtype case class AdParam(value: String) {
    def toDomain: Option[AdId] =
      Try(UUID.fromString(value)).map(AdId.apply).toOption
  }

  object AdParam {
    implicit val jsonEncoder: Encoder[AdParam] =
      Encoder.forProduct1("id")(_.value)
    implicit val jsonDecoder: Decoder[AdParam] =
      Decoder.forProduct1("id")(AdParam.apply)
  }

  final case class InvalidAdId(id: AdId)        extends NoStackTrace
  final case class CannotCreateChatWithMyself() extends NoStackTrace
  final case class ChatAlreadyExists()          extends NoStackTrace
  final case class NotAnAuthor()                extends NoStackTrace
}
