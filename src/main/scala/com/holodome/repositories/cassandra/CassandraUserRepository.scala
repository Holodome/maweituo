package com.holodome.repositories.cassandra

import cats.data.OptionT
import cats.effect.{Async, IO}
import cats.syntax.all._
import com.holodome.domain.User
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.holodome.repositories.UserRepository
import com.outworkers.phantom.dsl._

import java.util.UUID

object CassandraUserRepository {
  def make[F[_]: Async](db: UsersDatabase): UserRepository[F] =
    new CassandraUserRepository(
      db
    )
}

sealed class CassandraUserRepository[F[_]: Async] private (db: UsersDatabase)
    extends UserRepository[F] {
  import db.{session, space}

  override def create(request: User.CreateUser): F[UUID] = ???

  override def all(): F[List[User]] =
    Async[F].fromFuture(db.users.select.all().fetch().pure[F])

  override def find(userId: UUID): OptionT[F, User] = ???

  override def findByEmail(email: String): OptionT[F, User] = ???

  override def findByName(name: String): OptionT[F, User] = ???

}
