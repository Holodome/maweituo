package com.holodome.repositories.cassandra

import cats.data.OptionT
import cats.effect.IO
import com.holodome.domain.User
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.holodome.repositories.UserRepository
import com.outworkers.phantom.dsl._

import java.util.UUID

class CassandraUserRepository(db: UsersDatabase) extends UserRepository[IO] {
  import db.{session, space}

  override def create(request: User.CreateUser): IO[UUID] = ???

  override def all(): IO[List[User]] =
    IO.fromFuture(IO(db.users.select.all().fetch()))

  override def find(userId: UUID): OptionT[IO, User] = ???

  override def findByEmail(email: String): OptionT[IO, User] = ???

  override def findByName(name: String): OptionT[IO, User] = ???

}
