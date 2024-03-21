package com.holodome.services

import com.holodome.domain.users._
import com.holodome.repositories.UserRepository
import cats._
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.auth.PasswordHashing
import com.holodome.domain.Id

import java.time.LocalDateTime

trait UserService[F[_]] {
  def find(id: UserId): OptionT[F, User]

  def login(
      body: LoginRequest
  ): F[UserId]

  def register(
      body: RegisterRequest
  ): F[Unit]
}

object UserService {
  private final class UserServiceInterpreter[F[_]: MonadThrow: Sync](
      repo: UserRepository[F]
  ) extends UserService[F] {
    override def find(id: UserId): OptionT[F, User] =
      repo.find(id)

    override def login(
        body: LoginRequest
    ): F[UserId] =
      repo
        .findByName(body.name)
        .getOrElseF(NoUserFound(body.name).raiseError[F, User])
        .flatMap {
          case u if passwordsMatch(u, body.password) => u.id.pure[F]
          case _                                     => InvalidPassword(body.name).raiseError[F, UserId]
        }

    override def register(
        body: RegisterRequest
    ): F[Unit] = {
      val user = for {
        _ <- repo
          .findByEmail(body.email)
          .getOrElse(UserEmailInUse(body.email).raiseError[F, Unit])
        _ <- repo
          .findByName(body.name)
          .getOrElse(UserNameInUse(body.name).raiseError[F, Unit])
        salt <- PasswordHashing.genSalt[F]
        id   <- Id.make[F, UserId]
        user = User(
          id,
          body.name,
          body.email,
          PasswordHashing.hashSaltPassword(body.password, salt),
          salt
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
