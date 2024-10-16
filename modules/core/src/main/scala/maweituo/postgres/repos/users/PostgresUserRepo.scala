package maweituo
package postgres
package repos
package users

import cats.Applicative
import cats.data.{NonEmptyList, OptionT}
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import doobie.postgres.implicits.given

object PostgresUserRepo:
  def make[F[_]: Async](xa: Transactor[F]): UserRepo[F] = new:

    def create(req: User): F[Unit] =
      sql"""
        insert into users (id, name, email, password, salt, created_at, updated_at) values
        (${req.id}::uuid, ${req.name}, ${req.email}, ${req.hashedPassword}, 
         ${req.salt}, ${req.createdAt}, ${req.updatedAt})
      """.update.run
        .transact(xa)
        .void

    def all: F[List[User]] =
      sql"select id, name, email, password, salt, created_at, updated_at from users"
        .query[User]
        .to[List]
        .transact(xa)

    def find(userId: UserId): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt, created_at, updated_at from users where id = $userId::uuid"
          .query[User]
          .option
          .transact(xa)
      )

    def findByEmail(email: Email): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt, created_at, updated_at from users where email = $email"
          .query[User]
          .option
          .transact(xa)
      )

    def findByName(name: Username): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt, created_at, updated_at from users where name = ${name}"
          .query[User]
          .option
          .transact(xa)
      )

    def delete(id: UserId): F[Unit] =
      sql"delete from users where id = $id::uuid".update.run.transact(xa).void

    def update(update: UpdateUserRepoRequest): F[Unit] =
      if update.email.isEmpty && update.name.isEmpty && update.password.isEmpty then
        Applicative[F].unit
      else
        updateQuery(update).update.run.transact(xa).void

    private def updateQuery(update: UpdateUserRepoRequest) =
      val sets = NonEmptyList(
        fr"updated_at = ${update.at}",
        List(
          update.name.map(_.value).map(name => fr" name = $name "),
          update.email.map(_.value).map(email => fr" email = $email "),
          update.password.map(_.value).map(password => fr" password = $password ")
        ).flatten
      )
      val id = update.id
      (fr"update users set " ++ sets.reduce(_ ++ fr"," ++ _) ++ fr" where id = $id::uuid")
