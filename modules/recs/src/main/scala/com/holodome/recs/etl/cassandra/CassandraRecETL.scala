package com.holodome.recs.etl.cassandra

import cats.effect.Async
import cats.syntax.all._
import cats.{Applicative, Parallel}
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cql.codecs._
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.recs.etl.RecETL
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

private class CassandraRecETL[F[_]: Async: Parallel](session: CassandraSession[F])
    extends RecETL[F] {

  private implicit class StreamMonoidSelect[A](stream: fs2.Stream.CompileOps[F, F, Set[A]]) {
    def oneSet: F[Set[A]] =
      stream.last.map(_.fold(Set[A]())(x => x))
  }

  override def saveToOBS(obs: ObjectStorage[F], objectId: ObjectId): F[Unit] =
    cql"select id, weights from recs.user_weights"
      .as[(UserId, List[Double])]
      .select(session)
      .map { case (uid, weights) =>
        s"$uid,${weights.map(_.toString).mkString(",")}"
      }
      .fold("") { case (str, it) =>
        str ++ it ++ "\n"
      }
      .head
      .compile
      .last
      .flatMap {
        case None      => (new RuntimeException).raiseError[F, Unit]
        case Some(csv) => obs.put(objectId, csv.getBytes)
      }

  override def run: F[Unit] =
    for {
      _    <- truncateAllSnapshotTables
      uids <- transferTelemetry
      tags <- storeTagsSnapshot
      _    <- takeAdsSnapshot(tags)
      _    <- calculateInterestWeights(uids, tags)
    } yield ()

  private def truncateAllSnapshotTables =
    (
      truncateAdsSnapshotQuery.execute(session).void,
      truncateAdsTagsQuery.execute(session).void,
      truncateUserWeightsQuery.execute(session).void,
      truncateUserDiscussedSnapshotQuery.execute(session).void,
      truncateUserCreatedSnapshotQuery.execute(session).void,
      truncateUserBoughtSnapshotQuery.execute(session).void
    ).parSequence_

  private def truncateAdsSnapshotQuery =
    cql"truncate recs.ads_snapshot".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def truncateAdsTagsQuery =
    cql"truncate recs.tags".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def truncateUserWeightsQuery =
    cql"truncate recs.user_weights".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def truncateUserDiscussedSnapshotQuery =
    cql"truncate recs.user_discussed_snapshot".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def truncateUserCreatedSnapshotQuery =
    cql"truncate recs.user_created_snapshot".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def truncateUserBoughtSnapshotQuery =
    cql"truncate recs.user_bought_snapshot".config(_.setConsistencyLevel(ConsistencyLevel.ALL))

  private def calculateInterestWeights(uids: List[UserId], tags: List[AdTag]): F[Unit] =
    uids.traverse_ { uid =>
      (
        cql"select ads from recs.user_bought_snapshot where id = $uid"
          .as[Set[AdId]]
          .select(session)
          .compile
          .oneSet,
        cql"select ads from recs.user_created_snapshot where id = $uid"
          .as[Set[AdId]]
          .select(session)
          .compile
          .oneSet,
        cql"select ads from recs.user_discussed_snapshot where id = $uid"
          .as[Set[AdId]]
          .select(session)
          .compile
          .oneSet
      ).parFlatMapN { case (bought, created, discussed) =>
        (
          bought.toList
            .traverse(selectAdTagFromSnapshot)
            .map(_.flatten)
            .map(_.fold(Set[AdTag]())((s, t) => s ++ t)),
          created.toList
            .traverse(selectAdTagFromSnapshot)
            .map(_.flatten)
            .map(_.fold(Set[AdTag]())((s, t) => s ++ t)),
          discussed.toList
            .traverse(selectAdTagFromSnapshot)
            .map(_.flatten)
            .map(_.fold(Set[AdTag]())((s, t) => s ++ t))
        ).parMapN { case (boughtTags, createdTags, discussedTags) =>
          val tagsMap = tags
            .foldLeft(scala.collection.mutable.Map[AdTag, Double]()) { case (m, t) =>
              m(t) = 0
              m
            }
          val boughtWeight    = 1.0
          val discussedWeight = 0.01
          val createdWeight   = 0.02
          for (t <- boughtTags) {
            tagsMap(t) += boughtWeight
          }
          for (t <- createdTags) {
            tagsMap(t) += createdWeight
          }
          for (t <- discussedTags) {
            tagsMap(t) += discussedWeight
          }
          tagsMap
        }.flatMap { weightMap =>
          val weightVector = tags.map(t => weightMap(t))
          cql"insert into recs.user_weights (id, weights) values ($uid, $weightVector)"
            .execute(session)
            .void
        }
      }
    }

  private def selectAdTagFromSnapshot(id: AdId) =
    cql"select tags from recs.ads_tags_snapshot where id = $id"
      .as[Set[AdTag]]
      .select(session)
      .head
      .compile
      .last

  private def takeAdsSnapshot(tags: List[AdTag]): F[Unit] =
    tags.traverse_ { tag =>
      for {
        ads <- selectAdsWithTagQuery(tag).select(session).compile.toList
        _   <- insertAdsSnapshotQuery(tag, ads).execute(session).void
      } yield ()
    }

  private def insertAdsSnapshotQuery(tag: AdTag, ads: List[AdId]) =
    cql"insert into recs.ads_snapshot (tag, ads) values ($tag, $ads)"

  private def selectAdsWithTagQuery(tag: AdTag) =
    cql"select id from local.advertisements where tags contains $tag".as[AdId]

  private def storeTagsSnapshot: F[List[AdTag]] =
    takeTagsSnapshot.flatTap { tags =>
      tags.foldLeftM(0)((idx, tag) =>
        storeTagsQuery(idx, tag)
          .execute(session)
          .map(_ => idx + 1)
      )
    }

  private def storeTagsQuery(index: Int, tag: AdTag) =
    cql"insert into recs.tags (index, tag) values ($index, $tag)"

  private def takeTagsSnapshot: F[List[AdTag]] =
    getAllTagsQuery
      .select(session)
      .fold(Set[AdTag]())((s, tag) => s + tag)
      .compile
      .last
      .map(_.fold(List[AdTag]())(_.toList))

  private def getAllTagsQuery =
    cql"select tags from local.advertisements".as[AdTag]

  private def transferTelemetry: F[List[UserId]] =
    (
      transferTransactionalBoughtTelemetry,
      transferTransactionalCreatedTelemetry,
      transferTransactionalDiscussedTelemetry
    ).parMapN((u1, u2, u3) => (u1 ++ u2 ++ u3).distinct)

  private def transferTransactionalBoughtTelemetry: F[List[UserId]] =
    transferTransactionalTelemetry(
      transactionalBoughtTelemetryUsers,
      transactionalBoughtFourUserTelemetry,
      boughtForUserInsert
    )

  private def transferTransactionalCreatedTelemetry: F[List[UserId]] =
    transferTransactionalTelemetry(
      transactionalCreatedTelemetryUsers,
      transactionalCreatedFourUserTelemetry,
      createdForUserInsert
    )

  private def transferTransactionalDiscussedTelemetry: F[List[UserId]] =
    transferTransactionalTelemetry(
      transactionalDiscussedTelemetryUsers,
      transactionalDiscussedFourUserTelemetry,
      discussedForUserInsert
    )

  private def transferTransactionalTelemetry(
      getUsers: => F[List[UserId]],
      getAdsForUser: UserId => F[Set[AdId]],
      insert: (UserId, Set[AdId]) => F[Unit]
  ): F[List[UserId]] =
    getUsers.flatTap { users =>
      users
        .traverse { uid =>
          (Applicative[F].pure(uid), getAdsForUser(uid)).tupled
        }
        .flatTap { inserts =>
          inserts.traverse_ { case (uid, ads) =>
            insert(uid, ads)
          }
        }
    }

  private def transactionalBoughtTelemetryUsers: F[List[UserId]] =
    cql"select id from recs.user_bought_transactional".as[UserId].select(session).compile.toList

  private def transactionalBoughtFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    cql"select ad from recs.user_bought_transactional where id = $userId"
      .as[AdId]
      .select(session)
      .compile
      .toList
      .map(_.toSet)

  private def boughtForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    cql"insert into recs.user_bought_snapshot (id, ads) values ($userId, $ads)"
      .execute(session)
      .void

  private def transactionalCreatedTelemetryUsers: F[List[UserId]] =
    cql"select id from recs.user_created_transactional".as[UserId].select(session).compile.toList

  private def transactionalCreatedFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    cql"select ad from recs.user_created_transactional where id = $userId"
      .as[AdId]
      .select(session)
      .compile
      .toList
      .map(_.toSet)

  private def createdForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    cql"insert into recs.user_created_snapshot (id, ads) values ($userId, $ads)"
      .execute(session)
      .void

  private def transactionalDiscussedTelemetryUsers: F[List[UserId]] =
    cql"select id from recs.user_discussed_transactional".as[UserId].select(session).compile.toList

  private def transactionalDiscussedFourUserTelemetry(userId: UserId): F[Set[AdId]] =
    cql"select ad from recs.user_discussed_transactional where id = $userId"
      .as[AdId]
      .select(session)
      .compile
      .toList
      .map(_.toSet)

  private def discussedForUserInsert(userId: UserId, ads: Set[AdId]): F[Unit] =
    cql"insert into recs.user_discussed_snapshot (id, ads) values ($userId, $ads)"
      .execute(session)
      .void
}
