package com.holodome.domain

import cats.Functor
import com.holodome.optics.uuidIso
import com.holodome.utils.EncodeRF
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.predicates.all.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.estatico.newtype.macros.newtype

import java.util.UUID

package object users {
  @derive(decoder, encoder, uuidIso, eqv, show)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder, show, eqv)
  @newtype case class Username(_value: NonEmptyString) {
    def value: String = _value.value
  }

  implicit def usernameEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, Username] = EncodeRF.map(Username.apply)

  type EmailT = String Refined MatchesRegex[W.`"""(^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$)"""`.T]
  @derive(decoder, encoder, show, eqv)
  @newtype case class Email(_value: EmailT) {
    def value: String = _value.value
  }

  implicit def emailEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, EmailT]
  ): EncodeRF[F, T, Email] = EncodeRF.map(Email.apply)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class Password(_value: NonEmptyString) {
    def value: String = _value.value
  }

  implicit def passwordEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, Password] = EncodeRF.map(Password.apply)

  @derive(decoder, encoder, eqv)
  @newtype
  case class HashedSaltedPassword(value: String)

  @derive(decoder, encoder, eqv)
  @newtype
  case class PasswordSalt(_value: NonEmptyString) {
    def value: String = _value.value
  }

  implicit def passwordSaltEncodeRF[F[_]: Functor, T](implicit
      E: EncodeRF[F, T, NonEmptyString]
  ): EncodeRF[F, T, PasswordSalt] = EncodeRF.map(PasswordSalt.apply)

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
