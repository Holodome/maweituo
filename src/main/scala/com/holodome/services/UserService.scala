package com.holodome.services

import com.holodome.domain.auth._
import com.holodome.domain.User
import com.holodome.repositories.UserRepository
import cats._
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.auth.PasswordHashing

import java.time.Instant
import java.util.UUID

trait UserService[F[_]] {
  def login(
      body: LoginRequest
  ): F[UUID]

  def register(
      body: RegisterRequest
  ): F[UUID]
}

object UserService {
  private class UserServiceInterpreter[F[_]: MonadThrow: Sync](
      repo: UserRepository[F]
  ) extends UserService[F] {
    override def login(
        body: LoginRequest
    ): F[UUID] =
      repo
        .findByName(body.name)
        .getOrElseF(NoUserFound(body.name).raiseError[F, User])
        .flatMap {
          case u if passwordsMatch(u, body.password) => u.id.pure[F]
          case _                                     => InvalidPassword(body.name).raiseError[F, UUID]
        }

    override def register(
        body: RegisterRequest
    ): F[UUID] = {
      val user = for {
        _ <- repo
          .findByEmail(body.email)
          .getOrElse(UserEmailInUse(body.email).raiseError[F, Unit])
        _ <- repo
          .findByName(body.name)
          .getOrElse(UserNameInUse(body.name).raiseError[F, Unit])
        salt <- PasswordHashing.genSalt[F]
        user = User.CreateUser(
          body.name,
          body.email,
          PasswordHashing.hashSaltPassword(body.password, salt),
          salt,
          Instant.now
        )
      } yield user
      user.flatMap(u => repo.create(u))
    }
  }

  def make[F[_]: MonadThrow: Sync](repo: UserRepository[F]): UserService[F] =
    new UserServiceInterpreter(repo)

  private def passwordsMatch(user: User, str: Password): Boolean =
    user.hashedPassword == PasswordHashing.hashSaltPassword(str, user.salt)
}
