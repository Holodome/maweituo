package com.holodome.interpreters

import com.holodome.auth.PasswordHashing
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.UserEmailInUse
import com.holodome.domain.errors.UserNameInUse
import com.holodome.domain.repositories.AdvertisementRepository
import com.holodome.domain.repositories.UserRepository
import com.holodome.domain.services.IAMService
import com.holodome.domain.services.UserService
import com.holodome.domain.users.*
import com.holodome.effects.GenUUID

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object UserServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      users: UserRepository[F],
      ads: AdvertisementRepository[F],
      iam: IAMService[F]
  ): UserService[F] = new:
    def update(update: UpdateUserRequest, authd: UserId): F[Unit] =
      for
        _   <- iam.authUserModification(update.id, authd)
        old <- users.get(update.id)
        updateUserInternal = UpdateUserInternal(
          update.id,
          update.name,
          update.email,
          update.password.map(
            PasswordHashing.hashSaltPassword(_, old.salt)
          )
        )
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

    def getAds(userId: UserId): F[List[AdId]] =
      ads.findIdsByAuthor(userId)
