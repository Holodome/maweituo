package com.holodome.cql

import com.holodome.domain.ads.{AdId, AdTag, AdTitle}
import com.holodome.domain.images.{ImageId, ImageUrl}
import com.holodome.domain.messages.{ChatId, MessageText}
import com.holodome.domain.users.{Email, HashedSaltedPassword, PasswordSalt, UserId, Username}
import com.ringcentral.cassandra4io.cql.Reads
import com.ringcentral.cassandra4io.cql.Reads.{stringReads, uuidReads}

object codecs {
  implicit val userIdReads: Reads[UserId]     = uuidReads.map(UserId.apply)
  implicit val usernameReads: Reads[Username] = stringReads.map(Username.apply)
  implicit val emailReads: Reads[Email]       = stringReads.map(Email.apply)
  implicit val passwordReads: Reads[HashedSaltedPassword] =
    stringReads.map(HashedSaltedPassword.apply)
  implicit val saltReads: Reads[PasswordSalt] = stringReads.map(PasswordSalt.apply)

  implicit val adIdReads: Reads[AdId]       = uuidReads.map(AdId.apply)
  implicit val adTitleReads: Reads[AdTitle] = stringReads.map(AdTitle.apply)
  implicit val adTagReads: Reads[AdTag]     = stringReads.map(AdTag.apply)

  implicit val imageIdReads: Reads[ImageId]   = uuidReads.map(ImageId.apply)
  implicit val imageUrlReads: Reads[ImageUrl] = stringReads.map(ImageUrl.apply)

  implicit val chatIdReads: Reads[ChatId]           = uuidReads.map(ChatId.apply)
  implicit val messageTextReads: Reads[MessageText] = stringReads.map(MessageText.apply)
}
