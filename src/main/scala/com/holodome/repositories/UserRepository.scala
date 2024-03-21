package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.users._

import java.util.UUID

trait UserRepository[F[_]] {
  def create(request: User): F[Unit]
  def all(): F[List[User]]
  def find(userId: UserId): OptionT[F, User]
  def findByEmail(email: Email): OptionT[F, User]
  def findByName(name: Username): OptionT[F, User]
}
