package com.holodome.interpreters

import com.holodome.auth.PasswordHashing
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.{ UserEmailInUse, UserNameInUse }
import com.holodome.domain.repositories.UserAdsRepository
import com.holodome.domain.repositories.UserRepository
import com.holodome.domain.services.{ IAMService, UserService }
import com.holodome.domain.users.*
import com.holodome.effects.GenUUID

import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object UserServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      repo: UserRepository[F],
      userAdRepo: UserAdsRepository[F],
      iam: IAMService[F]
  ): UserService[F] = new:
    def update(update: UpdateUserRequest, authorized: UserId): F[Unit] =
      for
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
      yield ()

    def delete(subject: UserId, authorized: UserId): F[Unit] =
      for
        _ <- iam.authorizeUserModification(subject, authorized)
        _ <- repo.delete(subject)
        _ <- Logger[F].info(s"Deleted user $subject by $authorized")
      yield ()

    def get(id: UserId): F[User] =
      repo.get(id)

    def create(
        body: RegisterRequest
    ): F[UserId] =
      for
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
      yield user.id

    def getAds(userId: UserId): F[Set[AdId]] =
      for
        x <- userAdRepo.get(userId).value
        v <- x match
          case Some(s) => s.pure[F]
          case None =>
            Logger[F]
              .warn(s"Tried to get ads for user $userId, but no such user is found") *> Set[AdId]()
              .pure[F]
      yield v
