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
  def find(id: Username): OptionT[F, User]

  def register(
      body: RegisterRequest
  ): F[Unit]
}

object UserService {

  def make[F[_]: MonadThrow: Sync](repo: UserRepository[F]): UserService[F] =
    new UserServiceInterpreter(repo)

  private final class UserServiceInterpreter[F[_]: MonadThrow: Sync](
      repo: UserRepository[F]
  ) extends UserService[F] {
    override def find(id: Username): OptionT[F, User] =
      repo.findByName(id)

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

}
