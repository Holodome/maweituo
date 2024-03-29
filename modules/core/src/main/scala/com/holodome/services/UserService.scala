package com.holodome.services

import cats._
import cats.data.OptionT
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
  def update(update: UpdateUserRequest, authorized: UserId): F[Unit]

  def register(
      body: RegisterRequest
  ): F[UserId]
}

object UserService {

  def make[F[_]: MonadThrow: GenUUID](repo: UserRepository[F], iam: IAMService[F]): UserService[F] =
    new UserServiceInterpreter(repo, iam)

  private final class UserServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: UserRepository[F],
      iam: IAMService[F]
  ) extends UserService[F] {

    override def update(update: UpdateUserRequest, authorized: UserId): F[Unit] =
      iam.authorizeUserModification(update.id, authorized) >> {
        for {
          old <- find(update.id)
          updateUserInternal = UpdateUserInternal(
            update.id,
            update.name,
            update.email,
            update.password.map(
              PasswordHashing.hashSaltPassword(_, old.salt)
            )
          )
          _ <- repo.update(updateUserInternal)
        } yield ()
      }

    def delete(subject: UserId, authorized: UserId): F[Unit] =
      iam.authorizeUserModification(subject, authorized) >> repo.delete(subject)

    override def find(id: UserId): F[User] =
      repo.find(id).getOrRaise(InvalidUserId())

    override def findByName(name: Username): F[User] =
      repo.findByName(name).getOrRaise(NoUserFound(name))

    override def register(
        body: RegisterRequest
    ): F[UserId] = {
      val user = for {
        _ <- repo
          .findByEmail(body.email)
          .flatMap(_ => OptionT liftF UserEmailInUse(body.email).raiseError[F, Unit])
          .value
        _ <- repo
          .findByName(body.name)
          .flatMap(_ => OptionT liftF UserNameInUse(body.name).raiseError[F, Unit])
          .value
        salt <- PasswordHashing.genSalt[F]
        id   <- Id.make[F, UserId]
        user = User(
          id,
          body.name,
          body.email,
          PasswordHashing.hashSaltPassword(body.password, salt),
          salt,
          Set()
        )
      } yield user
      user.flatTap(repo.create).map(_.id)
    }
  }

}
