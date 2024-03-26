package com.holodome.http

import cats.syntax.all._
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId

import java.util.UUID

object vars {
  protected class UUIDVar[A](f: UUID => A) {
    def unapply(str: String): Option[A] =
      Either
        .catchNonFatal(f(UUID.fromString(str)))
        .toOption
  }

  object UserIdVar  extends UUIDVar(UserId.apply)
  object AdIdVar    extends UUIDVar(AdvertisementId.apply)
  object ChatIdVar  extends UUIDVar(ChatId.apply)
  object ImageIdVar extends UUIDVar(ImageId.apply)
}
