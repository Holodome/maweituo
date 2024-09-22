package com.holodome.domain.ads

import java.util.UUID

import scala.util.Try

import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.utils.{ IdNewtype, Newtype }

import cats.Show
import cats.derived.*
import cats.kernel.Eq
import cats.syntax.all.*
import io.circe.Codec
import io.circe.{ Decoder, Encoder }

type AdId = AdId.Type
object AdId extends IdNewtype

type AdTitle = AdTitle.Type
object AdTitle extends Newtype[String]

type AdTag = AdTag.Type
object AdTag extends Newtype[String]

final case class Advertisement(
    id: AdId,
    authorId: UserId,
    title: AdTitle,
    tags: Set[AdTag],
    images: Set[ImageId],
    chats: Set[ChatId],
    resolved: Boolean
) derives Show,
      Codec.AsObject

final case class CreateAdRequest(
    title: AdTitle
) derives Codec.AsObject

final case class AddTagRequest(
    tag: AdTag
) derives Show, Codec.AsObject

final case class AdParam(value: String) derives Eq, Show:
  def toDomain: Option[AdId] =
    Try(UUID.fromString(value)).map(AdId.apply).toOption
