package com.holodome.services

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.auth.PasswordHashing
import com.holodome.domain.Id
import com.holodome.domain.errors.UserEmailInUse
import com.holodome.domain.errors.UserNameInUse
import com.holodome.domain.users.RegisterRequest
import com.holodome.domain.users.UpdateUserInternal
import com.holodome.domain.users.UpdateUserRequest
import com.holodome.domain.users.User
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository
import org.typelevel.log4cats.Logger

object UserServiceInterpreter {

  def make[F[_]: MonadThrow: GenUUID: Logger](
      repo: UserRepository[F],
      iam: IAMService[F]
  ): UserService[F] =
    new UserServiceInterpreter(repo, iam)

}

private final class UserServiceInterpreter[F[_]: MonadThrow: GenUUID: Logger](
    repo: UserRepository[F],
    iam: IAMService[F]
) extends UserService[F] {

  override def update(update: UpdateUserRequest, authorized: UserId): F[Unit] = for {
    _   <- iam.authorizeUserModification(update.id, authorized)
    old <- repo.get(update.id)
    updateUserInternal = UpdateUserInternal(
      update.id,
      update.name,
      update.email,
      update.password.map(
        PasswordHashing.hashSaltPassword(_, old.salt)
      )
    )
    _ <- repo.update(updateUserInternal)
    _ <- Logger[F].info(s"Updated user ${update.id} by user $authorized")
  } yield ()

  def delete(subject: UserId, authorized: UserId): F[Unit] = for {
    _ <- iam.authorizeUserModification(subject, authorized)
    _ <- repo.delete(subject)
    _ <- Logger[F].info(s"Deleted user $subject by $authorized")
  } yield ()

  override def get(id: UserId): F[User] =
    repo.get(id)

  override def create(
      body: RegisterRequest
  ): F[UserId] = for {
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
      salt
    )
    _ <- repo.create(user)
    _ <- Logger[F].info(s"Created user $id")
  } yield user.id
}
