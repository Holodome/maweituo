package maweituo.postgres.repos.users

import maweituo.domain.users.*
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.sql.codecs.given

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import cats.Applicative
import cats.effect.kernel.Sync

object PostgresUserRepo:
  def make[F[_]: Async](xa: Transactor[F]): UserRepo[F] = new:

    def create(req: User): F[Unit] =
      sql"""
        insert into users (id, name, email, password, salt) values
        (${req.id}::uuid, ${req.name}, ${req.email}, ${req.hashedPassword}, ${req.salt})
      """.update.run
        .transact(xa)
        .void

    def all: F[List[User]] =
      sql"select id, name, email, password, salt from users"
        .query[User]
        .to[List]
        .transact(xa)

    def find(userId: UserId): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt from users where id = $userId::uuid"
          .query[User]
          .option
          .transact(xa)
      )

    def findByEmail(email: Email): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt from users where email = $email"
          .query[User]
          .option
          .transact(xa)
      )

    def findByName(name: Username): OptionT[F, User] =
      OptionT(
        sql"select id, name, email, password, salt from users where name = ${name}"
          .query[User]
          .option
          .transact(xa)
      )

    def delete(id: UserId): F[Unit] =
      sql"delete from users where id = $id::uuid".update.run.transact(xa).void

    def update(update: UpdateUserInternal): F[Unit] =
      if update.email.isEmpty && update.name.isEmpty && update.password.isEmpty then
        Applicative[F].unit
      else
        updateQuery(update).update.run.transact(xa).void.onError {
          case e: java.sql.SQLException =>
            Sync[F].delay(println(e))
        }

    private def updateQuery(update: UpdateUserInternal) =
      val sets = List(
        update.name.map(_.value).map(name => fr" name = $name "),
        update.email.map(_.value).map(email => fr" email = $email "),
        update.password.map(_.value).map(password => fr" password = $password ")
      ).map(_.toList).flatten
      assert(sets.nonEmpty)
      val id = update.id
      (fr"update users set " ++ sets.reduce(_ ++ fr"," ++ _) ++ fr" where id = $id::uuid")
