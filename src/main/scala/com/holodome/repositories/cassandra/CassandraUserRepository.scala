package com.holodome.repositories.cassandra

import cats.data.OptionT
import cats.effect.{Async, IO}
import cats.syntax.all._
import com.holodome.domain.users._
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.holodome.repositories.UserRepository
import com.outworkers.phantom.builder.syntax.CQLSyntax.eqs
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

  override def create(value: User): F[Unit] =
    Async[F].fromFuture(
      db.users
        .insert()
        .value(_.id, value.id.value)()
        .value(_.name, value.name.value)()
        .value(_.email, value.email.value)()
        .value(_.password, value.hashedPassword.value)()
        .value(_.salt, value.salt.value)()
        .future()
        .map(_ => ())
        .pure[F]
    )

  override def all(): F[List[User]] =
    Async[F].fromFuture(db.users.select.all().fetch().pure[F])

  override def find(userId: UserId): OptionT[F, User] =
    OptionT(Async[F].fromFuture(db.users.select.where(_.id eqs userId.value).one().pure[F]))

  override def findByEmail(email: Email): OptionT[F, User] =
    OptionT(Async[F].fromFuture(db.users.select.where(_.email eqs email.value).one().pure[F]))

  override def findByName(name: Username): OptionT[F, User] =
    OptionT(Async[F].fromFuture(db.users.select.where(_.name eqs name.value).one().pure[F]))

}
