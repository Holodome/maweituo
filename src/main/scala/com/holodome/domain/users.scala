package com.holodome.domain

import derevo.cats.eqv
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all._
import io.circe.refined._

import scala.util.control.NoStackTrace
import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID

object users {
  @derive(decoder, encoder)
  @newtype case class Username(value: String)

  private type EmailT = String Refined MatchesRegex["""[a-z0-9]+@[a-z0-9]+\\.[a-z0-9]{2,}"""]

  @derive(decoder, encoder)
  @newtype case class Email(value: EmailT)

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
  final case class LoginRequest(name: Username, password: Password)

  @derive(decoder, encoder)
  final case class RegisterRequest(
      name: Username,
      email: Email,
      password: Password
  )

  case class NoUserFound(username: Username)     extends NoStackTrace
  case class UserNameInUse(username: Username)   extends NoStackTrace
  case class UserEmailInUse(email: Email)        extends NoStackTrace
  case class InvalidPassword(username: Username) extends NoStackTrace

  case class User(
      id: UUID,
      name: Username,
      email: Email,
      hashedPassword: HashedSaltedPassword,
      salt: PasswordSalt,
      createdAt: Instant,
      updatedAt: Instant
  )

  case class CreateUser(
      name: Username,
      email: Email,
      password: HashedSaltedPassword,
      salt: PasswordSalt,
      time: Instant
  )
}
