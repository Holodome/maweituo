package com.holodome.cassandra.repositories

import cats.effect.Async
import cats.syntax.all._
import com.holodome.domain.{ads, users}
import com.holodome.domain.repositories.TelemetryRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraTelemetryRepository {
  def make[F[_]: Async](session: CassandraSession[F]): TelemetryRepository[F] =
    new CassandraTelemetryRepository(session)
}

private final class CassandraTelemetryRepository[F[_]: Async](session: CassandraSession[F])
    extends TelemetryRepository[F] {

  override def userCreated(user: users.UserId, ad: ads.AdId): F[Unit] =
    cql"insert into rec.user_created_transactional (id, ad) values (${user.value}, ${ad.value})"
      .execute(session)
      .void

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] =
    cql"insert into rec.user_bought_transactional (id, ad) values (${user.value}, ${ad.value})"
      .execute(session)
      .void

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] =
    cql"insert into rec.user_discussed_transactional (id, ad) values (${user.value}, ${ad.value})"
      .execute(session)
      .void

}