package maweituo
package logic
package interp
package users

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.infrastructure.effects.{GenUUID, TimeSource}
import maweituo.logic.auth.PasswordHashing
import maweituo.utils.Id

import org.typelevel.log4cats.syntax.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

object UserServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: LoggerFactory: TimeSource](
      users: UserRepo[F]
  )(using iam: IAMService[F]): UserService[F] = new:
    private given Logger[F] = LoggerFactory[F].getLogger

    def update(update: UpdateUserRequest)(using Identity): F[Unit] =
      for
        _       <- iam.authUserModification(update.id)
        old     <- users.get(update.id)
        updRepo <- UpdateUserRepoRequest.fromReq(update, old.salt)
        _       <- users.update(updRepo)
        _       <- info"Updated user ${update.id} by user ${summon[Identity]}"
      yield ()

    def delete(subject: UserId)(using Identity): F[Unit] =
      for
        _ <- iam.authUserModification(subject)
        _ <- users.delete(subject)
        _ <- info"Deleted user $subject by ${summon[Identity]}"
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
          .flatMap(_ => OptionT liftF DomainError.UserEmailInUse(body.email).raiseError[F, Unit])
          .value
        _ <- users
          .findByName(body.name)
          .flatMap(_ => OptionT liftF DomainError.UserNameInUse(body.name).raiseError[F, Unit])
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
        _ <- info"Created user $id"
      yield user.id
