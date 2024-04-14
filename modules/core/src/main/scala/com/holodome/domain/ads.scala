package com.holodome.domain

import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.queryParam
import com.holodome.optics.{stringIso, uuidIso}
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.Try
import scala.util.control.NoStackTrace

object ads {
  @derive(decoder, encoder, uuidIso, eqv)
  @newtype case class AdId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class AdTitle(value: String)

  @derive(decoder, encoder, eqv, stringIso, show)
  @newtype case class AdTag(value: String)

  @derive(encoder)
  case class Advertisement(
      id: AdId,
      title: AdTitle,
      tags: Set[AdTag],
      images: Set[ImageId],
      chats: Set[ChatId],
      authorId: UserId,
      resolved: Boolean
  )

  @derive(decoder, encoder, show)
  case class CreateAdRequest(
      title: AdTitle
  )

  @derive(decoder, encoder, show)
  case class AddTagRequest(
      tag: AdTag
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

}
