package com.holodome.http

import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.utils.EncodeR

import java.util.UUID
import scala.util.Try

package object vars {
  sealed class UUIDVar[A](f: UUID => A) {
    def unapply(str: String): Option[A] =
      Try(UUID.fromString(str)).toOption.map(f)
  }

  sealed class StringVar[A](implicit E: EncodeR[String, A]) {
    def unapply(str: String): Option[A] =
      E.encodeRF(str).toOption
  }

  object UserIdVar  extends UUIDVar(UserId.apply)
  object AdIdVar    extends UUIDVar(AdId.apply)
  object ChatIdVar  extends UUIDVar(ChatId.apply)
  object ImageIdVar extends UUIDVar(ImageId.apply)

  object TagVar extends StringVar[AdTag]
}
