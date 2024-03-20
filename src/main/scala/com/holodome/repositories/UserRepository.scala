package com.holodome.repositories

import cats.Applicative
import cats.data.OptionT
import cats.effect.IO
import com.holodome.domain.User

import java.util.UUID

abstract class UserRepository[F[_]: Applicative] {
  def create(request: User.CreateUser): F[UUID]
  def all(): F[List[User]]
  def find(userId: UUID): OptionT[F, User]
  def findByEmail(email: String): OptionT[F, User]

  def findByName(name: String): OptionT[F, User]
}
