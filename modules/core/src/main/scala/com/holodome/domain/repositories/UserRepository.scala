package com.holodome.domain.repositories

import com.holodome.domain.errors.*
import com.holodome.domain.users.*

import cats.MonadThrow
import cats.data.OptionT

trait UserRepository[F[_]]:
  def create(request: User): F[Unit]
  def all: F[List[User]]
  def find(userId: UserId): OptionT[F, User]
  def findByEmail(email: Email): OptionT[F, User]
  def findByName(name: Username): OptionT[F, User]
  def delete(id: UserId): F[Unit]
  def update(update: UpdateUserInternal): F[Unit]

object UserRepository:
  extension [F[_]: MonadThrow](repo: UserRepository[F])
    def get(userId: UserId): F[User] =
      repo.find(userId).getOrRaise(InvalidUserId(userId))

    def getByName(name: Username): F[User] =
      repo.findByName(name).getOrRaise(NoUserFound(name))

    def getByEmail(email: Email): F[User] =
      repo.findByEmail(email).getOrRaise(InvalidEmail(email))
