package maweituo.interp.users

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*

import maweituo.auth.PasswordHashing
import maweituo.domain.errors.{UserEmailInUse, UserNameInUse}
import maweituo.domain.services.IAMService
import maweituo.domain.users.*
import maweituo.domain.users.UpdateUserRepoRequest.fromReq
import maweituo.domain.users.repos.UserRepo
import maweituo.domain.users.services.UserService
import maweituo.domain.{Id, Identity}
import maweituo.effects.GenUUID

import org.typelevel.log4cats.Logger
import maweituo.effects.TimeSource

object UserServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      users: UserRepo[F]
  )(using iam: IAMService[F]): UserService[F] = new:
    def update(update: UpdateUserRequest)(using Identity): F[Unit] =
      for
        _       <- iam.authUserModification(update.id)
        old     <- users.get(update.id)
        updRepo <- UpdateUserRepoRequest.fromReq(update, old.salt)
        _       <- users.update(updRepo)
        _       <- Logger[F].info(s"Updated user ${update.id} by user ${summon[Identity]}")
      yield ()

    def delete(subject: UserId)(using Identity): F[Unit] =
      for
        _ <- iam.authUserModification(subject)
        _ <- users.delete(subject)
        _ <- Logger[F].info(s"Deleted user $subject by ${summon[Identity]}")
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
        at   <- TimeSource[F].instant
        user = User(
          id,
          body.name,
          body.email,
          PasswordHashing.hashSaltPassword(body.password, salt),
          salt,
          at,
          at
        )
        _ <- users.create(user)
        _ <- Logger[F].info(s"Created user $id")
      yield user.id
