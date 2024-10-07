package maweituo.domain.users

import java.time.Instant

import cats.kernel.Eq
import cats.syntax.all.*
import cats.{Functor, Show}

import maweituo.auth.PasswordHashing
import maweituo.effects.TimeSource
import maweituo.utils.{IdNewtype, Newtype}

import dev.profunktor.auth.jwt.{JwtSymmetricAuth, JwtToken}
import io.circe.Decoder

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
)

final case class User(
    id: UserId,
    name: Username,
    email: Email,
    hashedPassword: HashedSaltedPassword,
    salt: PasswordSalt,
    createdAt: Instant,
    updatedAt: Instant
)

final case class AuthedUser(id: UserId)
final case class UserJwtAuth(value: JwtSymmetricAuth)

final case class UpdateUserRequest(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[Password]
)

final case class UpdateUserRepoRequest(
    id: UserId,
    name: Option[Username],
    email: Option[Email],
    password: Option[HashedSaltedPassword],
    at: Instant
)

object UpdateUserRepoRequest:
  def fromReq[F[_]: TimeSource: Functor](req: UpdateUserRequest, salt: PasswordSalt): F[UpdateUserRepoRequest] =
    TimeSource[F].instant.map { at =>
      UpdateUserRepoRequest(
        req.id,
        req.name,
        req.email,
        req.password.map(
          PasswordHashing.hashSaltPassword(_, salt)
        ),
        at
      )
    }
