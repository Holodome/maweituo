package maweituo
package postgres

import java.util.UUID

import maweituo.domain.all.*

import doobie.util.meta.Meta

object codecs:
  given Meta[UUID] = Meta[String].imap[UUID](UUID.fromString)(_.toString)

  given Meta[UserId]               = Meta[UUID].timap(UserId.apply)(_.value)
  given Meta[Username]             = Meta[String].timap(Username.apply)(_.value)
  given Meta[Email]                = Meta[String].timap(Email.apply)(_.value)
  given Meta[HashedSaltedPassword] = Meta[String].timap(HashedSaltedPassword.apply)(_.value)
  given Meta[PasswordSalt]         = Meta[String].timap(PasswordSalt.apply)(_.value)

  given Meta[AdId]  = Meta[UUID].timap(AdId.apply)(_.value)
  given Meta[AdTag] = Meta[String].timap(AdTag.apply)(_.value)

  given Meta[AdTitle]     = Meta[String].timap(AdTitle.apply)(_.value)
  given Meta[ChatId]      = Meta[UUID].timap(ChatId.apply)(_.value)
  given Meta[MessageText] = Meta[String].timap(MessageText.apply)(_.value)

  given Meta[ImageId]  = Meta[UUID].timap(ImageId.apply)(_.value)
  given Meta[ImageUrl] = Meta[String].timap(ImageUrl.apply)(_.value)