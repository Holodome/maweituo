package com.holodome.cassandra

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cql.codecs._
import com.holodome.domain.repositories.UserRepository
import com.holodome.domain.users._
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql._

object CassandraUserRepository {
  def make[F[_]: Async](session: CassandraSession[F]): UserRepository[F] =
    new CassandraUserRepository(session)
}

private final class CassandraUserRepository[F[_]: Async](session: CassandraSession[F])
    extends UserRepository[F] {

  override def create(request: User): F[Unit] = createQuery(request).execute(session).void

  override def all: F[List[User]] =
    allQuery.select(session).compile.toList

  override def find(userId: UserId): OptionT[F, User] =
    OptionT(findQuery(userId).select(session).head.compile.last)

  override def findByEmail(email: Email): OptionT[F, User] =
    OptionT(findByEmailQuery(email).select(session).head.compile.last)

  override def findByName(name: Username): OptionT[F, User] =
    OptionT(findByNameQuery(name).select(session).head.compile.last)

  override def delete(id: UserId): F[Unit] =
    deleteQuery(id).execute(session).void

  override def update(update: UpdateUserInternal): F[Unit] =
    updateQuery(update).execute(session).void

  private def createQuery(req: User) =
    cql"""insert into users (id, name, email, password, salt) values
         |(${req.id}, ${req.name.value}, ${req.email.value},
         |${req.hashedPassword.value}, ${req.salt.value})""".stripMargin
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )

  private def allQuery =
    cql"select id, name, email, password, salt from users"
      .as[User]

  private def findQuery(id: UserId) =
    cql"select id, name, email, password, salt from users where id = $id"
      .as[User]

  private def findByEmailQuery(email: Email) = {
    cql"select id, name, email, password, salt from users where email = ${email.value}"
      .as[User]
  }

  private def findByNameQuery(name: Username) = {
    cql"select id, name, email, password, salt from users where name = ${name.value}"
      .as[User]
  }

  private def deleteQuery(id: UserId) =
    cql"delete from users where id = $id".config(
      _.setConsistencyLevel(ConsistencyLevel.QUORUM)
    )

  private def updateQuery(update: UpdateUserInternal) = {
    val sets = List(
      update.name.map(_.value).fold(cql"")(name => cql"name = $name "),
      update.email.map(_.value).fold(cql"")(email => cql"email = $email "),
      update.password.map(_.value).fold(cql"")(password => cql"password = $password ")
    )
    assert(sets.nonEmpty)
    val id = update.id
    (cql"update users set " ++ sets.reduce(_ ++ cql"," ++ _) ++ cql" where id = $id").config(
      _.setConsistencyLevel(ConsistencyLevel.QUORUM)
    )
  }
}
