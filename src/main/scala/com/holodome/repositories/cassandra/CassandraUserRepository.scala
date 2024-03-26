package com.holodome.repositories.cassandra

import cats.data.OptionT
import cats.effect.Async
import com.holodome.domain.users._
import com.holodome.ext.catsInterop.liftFuture
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.holodome.repositories.UserRepository
import com.outworkers.phantom.dsl._

object CassandraUserRepository {
  def make[F[_]: Async](db: UsersDatabase): UserRepository[F] =
    new CassandraUserRepository(db)
}

sealed class CassandraUserRepository[F[_]: Async] private (db: UsersDatabase)
    extends UserRepository[F] {
  import db.{session, space}

  override def create(value: User): F[Unit] =
    liftFuture(
      db.users
        .insert()
        .value(_.id, value.id.value)()
        .value(_.name, value.name.value)()
        .value(_.email, value.email.value)()
        .value(_.password, value.hashedPassword.value)()
        .value(_.salt, value.salt.value)()
        .future()
        .map(_ => ())
    )

  override def all(): F[List[User]] =
    liftFuture(db.users.select.all().fetch())

  override def find(userId: UserId): OptionT[F, User] =
    OptionT(
      liftFuture(db.users.select.where(_.id eqs userId.value).one())
    )

  override def findByEmail(email: Email): OptionT[F, User] =
    OptionT(
      liftFuture(db.users.select.where(_.email eqs email.value).one())
    )

  override def findByName(name: Username): OptionT[F, User] =
    OptionT(liftFuture(db.users.select.where(_.name eqs name.value).one()))

  override def delete(id: UserId): F[Unit] =
    liftFuture(db.users.delete().where(_.id eqs id.value).future().map(_ => ()))

  override def update(update: UpdateUserInternal): F[Unit] = ???
}
