package com.holodome.repositories

import cats.MonadThrow
import cats.data.OptionT
import com.holodome.domain.errors.InvalidUserId
import com.holodome.domain.errors.NoUserFound
import com.holodome.domain.users._

trait UserRepository[F[_]] extends {
  def create(request: User): F[Unit]
  def all: F[List[User]]
  def find(userId: UserId): OptionT[F, User]
  def findByEmail(email: Email): OptionT[F, User]
  def findByName(name: Username): OptionT[F, User]
  def delete(id: UserId): F[Unit]
  def update(update: UpdateUserInternal): F[Unit]
}

object UserRepository {
  implicit class UserRepositoryOps[F[_]: MonadThrow](repo: UserRepository[F]) {
    def get(userId: UserId): F[User] =
      repo.find(userId).getOrRaise(InvalidUserId(userId))

    def getByName(name: Username): F[User] =
      repo.findByName(name).getOrRaise(NoUserFound(name))
  }
}
