package com.holodome.domain

import cats.instances.show
import derevo.cats.eqv
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

import scala.util.control.NoStackTrace
import io.estatico.newtype.macros.newtype

object auth {
  @derive(decoder, encoder, eqv)
  @newtype
  case class Password(value: String)

  @derive(decoder, encoder, eqv)
  @newtype
  case class HashedSaltedPassword(value: String)

  @derive(decoder, encoder, eqv)
  @newtype
  case class PasswordSalt(value: String)

  @derive(decoder, encoder)
  final case class LoginRequest(name: String, password: Password)

  @derive(decoder, encoder)
  final case class RegisterRequest(
      name: String,
      email: String,
      password: Password
  )

  case class NoUserFound(username: String) extends NoStackTrace
  case class UserNameInUse(username: String) extends NoStackTrace
  case class UserEmailInUse(email: String) extends NoStackTrace
  case class InvalidPassword(username: String) extends NoStackTrace
}
