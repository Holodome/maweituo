package com.holodome.domain

import com.holodome.optics.uuidIso
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import io.circe.refined._
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.predicates.all.MatchesRegex
import io.estatico.newtype.macros.newtype
import eu.timepit.refined.string._
import eu.timepit.refined.W

import java.util.UUID
import scala.util.control.NoStackTrace

object users {
  @derive(decoder, encoder, uuidIso, eqv, show)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder, show, eqv)
  @newtype case class Username(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype case class Email(
      value: String Refined MatchesRegex[W.`"""(?=[^\\s]+)(?=(\\w+)@([\\w\\.]+))"""`.T]
  )

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class Password(value: String)

  @derive(decoder, encoder, eqv)
  @newtype
  case class HashedSaltedPassword(value: String)

  @derive(decoder, encoder, eqv)
  @newtype
  case class PasswordSalt(value: String)

  @derive(decoder)
  final case class LoginRequest(name: Username, password: Password)

  @derive(decoder, encoder, show)
  final case class RegisterRequest(
      name: Username,
      email: Email,
      password: Password
  )

  case class InvalidUserId()                     extends NoStackTrace
  case class NoUserFound(username: Username)     extends NoStackTrace
  case class UserNameInUse(username: Username)   extends NoStackTrace
  case class UserEmailInUse(email: Email)        extends NoStackTrace
  case class InvalidPassword(username: Username) extends NoStackTrace
  case class InvalidAccess()                     extends NoStackTrace

  case class User(
      id: UserId,
      name: Username,
      email: Email,
      hashedPassword: HashedSaltedPassword,
      salt: PasswordSalt
  )

  @derive(encoder)
  case class UserPublicInfo(
      id: UserId,
      name: Username,
      email: Email
  )

  object UserPublicInfo {
    def fromUser(user: User): UserPublicInfo =
      UserPublicInfo(user.id, user.name, user.email)
  }

  case class AuthedUser(id: UserId)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @derive(decoder, show)
  case class UpdateUserRequest(
      id: UserId,
      name: Option[Username],
      email: Option[Email],
      password: Option[Password]
  )

  case class UpdateUserInternal(
      id: UserId,
      name: Option[Username],
      email: Option[Email],
      password: Option[HashedSaltedPassword]
  )
}
