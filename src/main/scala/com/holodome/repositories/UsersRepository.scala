package com.holodome.repositories

import com.holodome.repositories.models.User
import cats.effect.IO
import java.util.UUID

abstract class UsersRepository {
  def create(request: User.CreateUser): IO[Unit]
  def all(): IO[List[User]]
  def find(userId: UUID): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
}
