package com.holodome.services

import cats._
import cats.syntax.all._
import com.holodome.auth.PasswordHashing
import com.holodome.domain.users._
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository

trait UserService[F[_]] {
  def find(id: UserId): F[User]
  def findByName(name: Username): F[User]
  def delete(subject: UserId, authorized: UserId): F[Unit]
  def update(update: UpdateUser, authorized: UserId): F[Unit]

  def register(
      body: RegisterRequest
  ): F[UserId]
}

object UserService {

  def make[F[_]: MonadThrow: GenUUID](repo: UserRepository[F]): UserService[F] =
    new UserServiceInterpreter(repo)

  private final class UserServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: UserRepository[F]
  ) extends UserService[F] {

    override def update(update: UpdateUser, authorized: UserId): F[Unit] =
      if (authorized === update.id) {
        repo.update(update);
      } else {
        InvalidAccess().raiseError[F, Unit]
      }

    def delete(subject: UserId, authorized: UserId): F[Unit] =
      if (authorized === subject) {
        repo.delete(subject)
      } else {
        InvalidAccess().raiseError[F, Unit]
      }

    override def find(id: UserId): F[User] =
      repo.find(id).getOrElseF(InvalidUserId().raiseError)

    override def findByName(name: Username): F[User] =
      repo.findByName(name).getOrElseF(NoUserFound(name).raiseError)

    override def register(
        body: RegisterRequest
    ): F[UserId] = {
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
          salt,
          List()
        )
      } yield user
      user.flatTap(repo.create).map(_.id)
    }
  }

}
