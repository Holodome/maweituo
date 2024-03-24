package com.holodome.domain

import com.holodome.domain.users.UserId
import com.holodome.ext.http4s.queryParam
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.Try
import scala.util.control.NoStackTrace

object advertisements {
  @derive(decoder, encoder)
  @newtype case class AdvertisementId(value: UUID)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTitle(value: String)

  @derive(decoder, encoder)
  @newtype case class AdvertisementTag(value: String)

  @derive(decoder, encoder)
  case class Advertisement(
      id: AdvertisementId,
      title: AdvertisementTitle,
      tags: List[AdvertisementTag],
      authorId: UserId
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
}
