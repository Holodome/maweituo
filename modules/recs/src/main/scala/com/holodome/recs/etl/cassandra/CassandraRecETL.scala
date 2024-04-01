package com.holodome.recs.etl.cassandra

import cats.Parallel
import cats.effect.Async
import cats.syntax.all._
import cats.{Applicative, NonEmptyParallel}
import com.holodome.cql.codecs._
import com.holodome.ext.cassandra4io.typeMappers._
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.recs.etl.RecETL
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

private class CassandraRecETL[F[_]: Async: Parallel](session: CassandraSession[F])
    extends RecETL[F] {

  override def etlAll(): F[Unit] =
    (
      transferTelemetry,
      takeAdsSnapshot,
      takeTagsSnapshot
    ).parSequence_.void >> calculateInterestWeights

  private def calculateInterestWeights: F[Unit] = ???

  private def takeAdsSnapshot: F[Unit] = ???

  private def takeTagsSnapshot: F[Unit] = ???

  private def transferTelemetry: F[Unit] =
    (
      transferTransactionalBoughtTelemetry,
      transferTransactionalClickedTelemetry,
      transferTransactionalDiscussedTelemetry
    ).parSequence_.void

  private def transferTransactionalBoughtTelemetry: F[Unit] =
    transferTransactionalTelemetry(
      transactionalBoughtTelemetryUsers,
      transactionalBoughtFourUserTelemetry,
      boughtForUserInsert
    )

  private def transferTransactionalClickedTelemetry: F[Unit] =
    transferTransactionalTelemetry(
      transactionalClickedTelemetryUsers,
      transactionalClickedFourUserTelemetry,
      clickedForUserInsert
    )

  private def transferTransactionalDiscussedTelemetry: F[Unit] =
    transferTransactionalTelemetry(
      transactionalDiscussedTelemetryUsers,
      transactionalDiscussedFourUserTelemetry,
      discussedForUserInsert
    )

  private def transferTransactionalTelemetry(
      getUsers: => F[List[UserId]],
      getAdsForUser: UserId => F[Set[AdId]],
      insert: (UserId, Set[AdId]) => F[Unit]
  ): F[Unit] =
    getUsers.flatMap { users =>
      users
        .traverse { uid =>
          (Applicative[F].pure(uid), getAdsForUser(uid)).tupled
        }
        .flatMap { inserts =>
          inserts.traverse { case (uid, ads) =>
            insert(uid, ads)
          }
        }
    }.void

  private def transactionalBoughtTelemetryUsers: F[List[UserId]] =
    transactionalBoughtTelemetryQuery.select(session).compile.toList

  private def transactionalBoughtFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    transactionalBoughtForUser(userId).select(session).compile.toList.map(_.toSet)

  private def boughtForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    boughtForUserInsertQuery(userId, ads).execute(session).void

  private def transactionalBoughtTelemetryQuery =
    cql"select id from recs.user_bought_transactional".as[UserId]

  private def transactionalBoughtForUser(userId: UserId) =
    cql"select ad from recs.user_bought_transactional where id = $userId".as[AdId]

  private def boughtForUserInsertQuery(userId: UserId, ads: Set[AdId]) =
    cql"insert into recs.user_bought_snapshot (id, ads) values ($userId, $ads)"

  private def transactionalClickedTelemetryUsers: F[List[UserId]] =
    transactionalClickedTelemetryQuery.select(session).compile.toList

  private def transactionalClickedFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    transactionalClickedForUser(userId).select(session).compile.toList.map(_.toSet)

  private def clickedForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    clickedForUserInsertQuery(userId, ads).execute(session).void

  private def transactionalClickedTelemetryQuery =
    cql"select id from recs.user_clicked_transactional".as[UserId]

  private def transactionalClickedForUser(userId: UserId) =
    cql"select ad from recs.user_clicked_transactional where id = $userId".as[AdId]

  private def clickedForUserInsertQuery(userId: UserId, ads: Set[AdId]) =
    cql"insert into recs.user_clicked_snapshot (id, ads) values ($userId, $ads)"

  private def transactionalDiscussedTelemetryUsers: F[List[UserId]] =
    transactionalDiscussedTelemetryQuery.select(session).compile.toList

  private def transactionalDiscussedFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    transactionalDiscussedForUser(userId).select(session).compile.toList.map(_.toSet)

  private def discussedForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    discussedForUserInsertQuery(userId, ads).execute(session).void

  private def transactionalDiscussedTelemetryQuery =
    cql"select id from recs.user_discussed_transactional".as[UserId]

  private def transactionalDiscussedForUser(userId: UserId) =
    cql"select ad from recs.user_discussed_transactional where id = $userId".as[AdId]

  private def discussedForUserInsertQuery(userId: UserId, ads: Set[AdId]) =
    cql"insert into recs.user_discussed_snapshot (id, ads) values ($userId, $ads)"
}
