package com.holodome.postgres.sql.codecs

import java.util.UUID

import com.holodome.domain.ads.*
import com.holodome.domain.images.*
import com.holodome.domain.messages.*
import com.holodome.domain.users.*

import doobie.postgres.implicits.*
import doobie.util.meta.Meta

export CodecsOrphans.given

object CodecsOrphans:
  given Meta[UUID] = Meta[String].imap[UUID](UUID.fromString)(_.toString)

  given Meta[UserId]               = Meta[UUID].timap(UserId.apply)(_.value)
  given Meta[Username]             = Meta[String].timap(Username.apply)(_.value)
  given Meta[Email]                = Meta[String].timap(Email.apply)(_.value)
  given Meta[HashedSaltedPassword] = Meta[String].timap(HashedSaltedPassword.apply)(_.value)
  given Meta[PasswordSalt]         = Meta[String].timap(PasswordSalt.apply)(_.value)

  given Meta[AdId]    = Meta[UUID].timap(AdId.apply)(_.value)
  given Meta[ChatId]  = Meta[UUID].timap(ChatId.apply)(_.value)
  given Meta[ImageId] = Meta[UUID].timap(ImageId.apply)(_.value)
