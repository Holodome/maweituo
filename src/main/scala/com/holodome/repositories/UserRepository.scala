package com.holodome.repositories

import cats.Applicative
import cats.data.OptionT
import cats.effect.IO
import com.holodome.models.User

import java.util.UUID

abstract class UserRepository[F[_]: Applicative] {
  def create(request: User.CreateUser): OptionT[F, Unit]
  def all(): F[List[User]]
  def find(userId: UUID): OptionT[F, User]
  def findByEmail(email: String): OptionT[F, User]
}
