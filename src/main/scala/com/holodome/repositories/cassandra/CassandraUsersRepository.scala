package com.holodome.repositories.cassandra

import cats.effect.IO
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.holodome.repositories.UsersRepository
import com.holodome.repositories.models.User
import com.outworkers.phantom.dsl._

import java.util.UUID

class CassandraUsersRepository(db: UsersDatabase) extends UsersRepository {
  import db.{session, space}
  import com.holodome.AppContextShift._

  override def create(request: User.CreateUser): IO[Unit] = ???

  override def all(): IO[List[User]] =
    IO.fromFuture(IO(db.users.select.all().fetch()))

  override def find(userId: UUID): IO[Option[User]] = ???

  override def findByEmail(email: String): IO[Option[User]] = ???
}
