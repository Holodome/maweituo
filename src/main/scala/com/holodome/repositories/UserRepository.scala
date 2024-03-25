package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.users._

import java.util.UUID

trait UserRepository[F[_]] extends {
  def create(request: User): F[Unit]
  def all(): F[List[User]]
  def find(userId: UserId): OptionT[F, User]
  def findByEmail(email: Email): OptionT[F, User]
  def findByName(name: Username): OptionT[F, User]
  def delete(id: UserId): F[Unit]
  def update(update: UpdateUser): F[Unit]
}
