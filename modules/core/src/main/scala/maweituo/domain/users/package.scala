package maweituo.domain.users

import cats.Show
import cats.derived.*
import cats.kernel.Eq

import maweituo.auth.PasswordHashing
import maweituo.utils.{IdNewtype, Newtype}

import dev.profunktor.auth.jwt.{JwtSymmetricAuth, JwtToken}
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

final case class LoginRequest(name: Username, password: Password)
final case class LoginResponse(id: UserId, jwt: JwtToken)

final case class RegisterRequest(
    name: Username,
    email: Email,
    password: Password
) derives Show

final case class User(
    id: UserId,
    name: Username,
    email: Email,
    hashedPassword: HashedSaltedPassword,
    salt: PasswordSalt
)

final case class AuthedUser(id: UserId)
final case class UserJwtAuth(value: JwtSymmetricAuth)

final case class UpdateUserRequest(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[Password]
) derives Show

final case class UpdateUserRepoRequest(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[HashedSaltedPassword]
) derives Show

object UpdateUserRepoRequest:
  def fromReq(req: UpdateUserRequest, salt: PasswordSalt): UpdateUserRepoRequest =
    UpdateUserRepoRequest(
      req.id,
      req.name,
      req.email,
      req.password.map(
        PasswordHashing.hashSaltPassword(_, salt)
      )
    )
