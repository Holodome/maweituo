package maweituo.interpreters.users

import maweituo.auth.PasswordHashing
import maweituo.domain.Id
import maweituo.domain.errors.{UserEmailInUse, UserNameInUse}
import maweituo.domain.services.IAMService
import maweituo.domain.users.*
import maweituo.domain.users.UpdateUserInternal.fromReq
import maweituo.domain.users.repos.UserRepository
import maweituo.domain.users.services.UserService
import maweituo.effects.GenUUID

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object UserServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      users: UserRepository[F]
  )(using iam: IAMService[F]): UserService[F] = new:
    def update(update: UpdateUserRequest, authd: UserId): F[Unit] =
      for
        _   <- iam.authUserModification(update.id, authd)
        old <- users.get(update.id)
        updateUserInternal = UpdateUserInternal.fromReq(update, old.salt)
        _ <- users.update(updateUserInternal)
        _ <- Logger[F].info(s"Updated user ${update.id} by user $authd")
      yield ()

    def delete(subject: UserId, authd: UserId): F[Unit] =
      for
        _ <- iam.authUserModification(subject, authd)
        _ <- users.delete(subject)
        _ <- Logger[F].info(s"Deleted user $subject by $authd")
      yield ()

    def get(id: UserId): F[User] =
      users.get(id)

    def getByName(name: Username): F[User] =
      users.getByName(name)

    def getByEmail(email: Email): F[User] =
      users.getByEmail(email)

    def create(
        body: RegisterRequest
    ): F[UserId] =
      for
        _ <- users
          .findByEmail(body.email)
          .flatMap(_ => OptionT liftF UserEmailInUse(body.email).raiseError[F, Unit])
          .value
        _ <- users
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
        _ <- users.create(user)
        _ <- Logger[F].info(s"Created user $id")
      yield user.id
