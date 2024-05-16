package com.holodome.domain

import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.queryParam
import com.holodome.optics.{uuidIso}
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.Try
import cats.Functor
import com.holodome.utils.EncodeRF

package object ads {
  @derive(decoder, encoder, uuidIso, eqv)
  @newtype case class AdId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class AdTitle(value: NonEmptyString) {
    def str: String = value.value
  }

  implicit def adTitleEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, AdTitle] = EncodeRF.map(AdTitle.apply)

  @derive(decoder, encoder, eqv, show)
  @newtype case class AdTag(value: NonEmptyString) {
    def str: String = value.value
  }

  implicit def adTagEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, AdTag] = EncodeRF.map(AdTag.apply)

  @derive(encoder)
  case class Advertisement(
      id: AdId,
      authorId: UserId,
      title: AdTitle,
      tags: Set[AdTag],
      images: Set[ImageId],
      chats: Set[ChatId],
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
