package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.User

import java.util.UUID

abstract class UserRepository[F[_]] {
  def create(request: User.CreateUser): F[UUID]
  def all(): F[List[User]]
  def find(userId: UUID): OptionT[F, User]
  def findByEmail(email: String): OptionT[F, User]
  def findByName(name: String): OptionT[F, User]
}
