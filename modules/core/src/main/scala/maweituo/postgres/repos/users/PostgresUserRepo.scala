package maweituo.postgres.repos.users

import maweituo.domain.users.*
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.sql.codecs.given

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

object PostgresUserRepo:
  def make[F[_]: Async](xa: Transactor[F]): UserRepo[F] = new:

    def create(req: User): F[Unit] =
      sql"""
        insert into users (id, name, email, password, salt) values
        (${req.id}, ${req.name}, ${req.email}, ${req.hashedPassword}, ${req.salt})
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
        sql"select id, name, email, password, salt from users where id = $userId"
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
      sql"delete from users where id = $id".update.run.transact(xa).void

    def update(update: UpdateUserInternal): F[Unit] =
      updateQuery(update).update.run.transact(xa).void

    private def updateQuery(update: UpdateUserInternal) =
      val sets = List(
        update.name.map(_.value).fold(sql"")(name => sql"name = $name "),
        update.email.map(_.value).fold(sql"")(email => sql"email = $email "),
        update.password.map(_.value).fold(sql"")(password => sql"password = $password ")
      )
      assert(sets.nonEmpty)
      val id = update.id
      (sql"update users set " ++ sets.reduce(_ ++ sql"," ++ _) ++ sql" where id = $id")
