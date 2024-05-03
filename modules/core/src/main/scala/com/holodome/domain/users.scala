package com.holodome.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import io.circe.refined._
import derevo.derive
import com.holodome.optics.uuidIso
import dev.profunktor.auth.jwt.{JwtSymmetricAuth, JwtToken}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.predicates.all.MatchesRegex
import io.estatico.newtype.macros.newtype
import eu.timepit.refined.string._
import eu.timepit.refined.W
import com.holodome.domain._

import java.util.UUID
import scala.util.control.NoStackTrace

object users {
  @derive(decoder, encoder, uuidIso, eqv, show)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder, show, eqv)
  @newtype case class Username(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype case class Email(
      value: String Refined MatchesRegex[W.`"""(^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$)"""`.T]
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

  @derive(decoder, encoder, show)
  final case class LoginRequest(name: Username, password: Password)

  @derive(decoder, show, encoder)
  final case class RegisterRequest(
      name: Username,
      email: Email,
      password: Password
  )

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
