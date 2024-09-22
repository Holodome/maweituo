package com.holodome.postgres.sql

import com.holodome.domain.ads._
import com.holodome.domain.images._
import com.holodome.domain.messages._
import com.holodome.domain.users._
import com.holodome.utils.EncodeR
import java.util.UUID
import doobie.util.meta.Meta
import doobie.postgres.implicits._

package object codecs {
  private def encodeFail[T, A](t: T)(implicit E: EncodeR[T, A]): Either[String, A] =
    E.encodeRF(t).left.map(_.toString())

  implicit val userIdMeta: Meta[UserId]                 = Meta[UUID].timap(UserId.apply)(_.value)
  implicit val usernameMeta: Meta[Username]             = Meta[String].tiemap(encodeFail[String, Username])(_.value)
  implicit val emailMeta: Meta[Email]                   = Meta[String].tiemap(encodeFail[String, Email])(_.value)
  implicit val passwordMeta: Meta[HashedSaltedPassword] = Meta[String].timap(HashedSaltedPassword.apply)(_.value)
  implicit val saltMeta: Meta[PasswordSalt]             = Meta[String].tiemap(encodeFail[String, PasswordSalt])(_.value)

  implicit val adIdMeta: Meta[AdId]       = Meta[UUID].timap(AdId.apply)(_.value)
  implicit val chatIdMeta: Meta[ChatId]   = Meta[UUID].timap(ChatId.apply)(_.value)
  implicit val imageIdMeta: Meta[ImageId] = Meta[UUID].timap(ImageId.apply)(_.value)

}
