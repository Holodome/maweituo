package com.holodome.recs.repositories.cassandra

import cats.syntax.all._
import cats.effect.kernel.Async
import com.holodome.cql.codecs._
import com.holodome.domain.{ads, users}
import com.holodome.recs.repositories.TelemetryRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraTelemetryRepository {
  def make[F[_]: Async](session: CassandraSession[F]): TelemetryRepository[F] =
    new CassandraTelemetryRepository(session)
}

private final class CassandraTelemetryRepository[F[_]: Async](session: CassandraSession[F])
    extends TelemetryRepository[F] {

  override def userClicked(user: users.UserId, ad: ads.AdId): F[Unit] =
    userClickedQuery(user, ad).execute(session).void

  override def userBought(user: users.UserId, ad: ads.AdId): F[Unit] =
    userBoughtQuery(user, ad).execute(session).void

  override def userDiscussed(user: users.UserId, ad: ads.AdId): F[Unit] =
    userDiscussedQuery(user, ad).execute(session).void

  private def userClickedQuery(user: users.UserId, ad: ads.AdId) =
    cql"insert into rec.user_clicked_transactional (id, ad) values (${user.value}, ${ad.value})"

  private def userBoughtQuery(user: users.UserId, ad: ads.AdId) =
    cql"insert into rec.user_bought_transactional (id, ad) values (${user.value}, ${ad.value})"

  private def userDiscussedQuery(user: users.UserId, ad: ads.AdId) =
    cql"insert into rec.user_discussed_transactional (id, ad) values (${user.value}, ${ad.value})"

}
