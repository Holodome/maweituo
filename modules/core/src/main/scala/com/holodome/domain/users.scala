package com.holodome.domain.users

import com.holodome.utils.{ IdNewtype, Newtype }

import cats.Show
import cats.derived.*
import cats.kernel.Eq
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.Codec
import io.circe.Decoder
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

type UserId = UserId.Type
object UserId extends IdNewtype

type Username = Username.Type
object Username extends Newtype[String]

type Email = Email.Type
object Email extends Newtype[String]

type Password = Password.Type
object Password extends Newtype[String]

type HashedSaltedPassword = HashedSaltedPassword.Type
object HashedSaltedPassword extends Newtype[String]

type PasswordSalt = PasswordSalt.Type
object PasswordSalt extends Newtype[String]

final case class LoginRequest(name: Username, password: Password) derives Codec.AsObject

final case class RegisterRequest(
    name: Username,
    email: Email,
    password: Password
) derives Codec.AsObject

final case class User(
    id: UserId,
    name: Username,
    email: Email,
    hashedPassword: HashedSaltedPassword,
    salt: PasswordSalt
)

final case class UserPublicInfo(
    id: UserId,
    name: Username,
    email: Email
) derives Codec.AsObject, Show

object UserPublicInfo:
  def fromUser(user: User): UserPublicInfo =
    UserPublicInfo(user.id, user.name, user.email)

final case class AuthedUser(id: UserId)
final case class UserJwtAuth(value: JwtSymmetricAuth)

final case class UpdateUserRequest(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[Password]
) derives Codec.AsObject, Show

final case class UpdateUserInternal(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[HashedSaltedPassword]
) derives Codec.AsObject, Show
