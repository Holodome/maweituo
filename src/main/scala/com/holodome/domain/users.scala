package com.holodome.domain

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.optics.uuid
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.control.NoStackTrace

object users {
  @derive(decoder, encoder, uuid, eqv)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder, show)
  @newtype case class Username(value: String)

  @derive(decoder, encoder)
  @newtype case class Email(value: String)

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

  @derive(decoder, encoder)
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
      salt: PasswordSalt,
      ads: List[AdvertisementId]
  )

  @derive(encoder)
  case class UserPublicInfo(
      id: UserId,
      name: Username,
      email: Email,
      ads: List[AdvertisementId]
  )

  object UserPublicInfo {
    def fromUser(user: User): UserPublicInfo =
      UserPublicInfo(user.id, user.name, user.email, user.ads)
  }

  case class AuthedUser(id: UserId)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @derive(decoder)
  case class UpdateUser(
      id: UserId,
      name: Option[Username],
      email: Option[Email],
      password: Option[Password]
  )
}
