package com.holodome.recs.etl

import cats.effect.Async
import cats.syntax.all._
import cats.NonEmptyParallel
import com.holodome.domain.ads.{AdId, Advertisement}
import com.holodome.domain.users.{User, UserId}
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.ringcentral.cassandra4io.CassandraSession
import com.holodome.cql.codecs._

private class CassandraExtract[F[_]: Async: NonEmptyParallel](session: CassandraSession[F]) {

  def extract(locs: OBSSnapshotLocations, obs: ObjectStorage[F]) = Operator(obs).extract(locs)

  private final case class Operator(obs: ObjectStorage[F]) {
    def extract(locs: OBSSnapshotLocations): F[Unit] =
      (
        extractUsersToObs(locs.users),
        extractAdsToObs(locs.ads),
        extractUserBoughtToObs(locs.user_bought),
        extractUserCreatedToObs(locs.user_created),
        extractUserDiscussedToObs(locs.user_discussed)
      ).parMapN((_, _, _, _, _) => ())

    private def joinList(lst: List[String])  = lst.mkString(",")
    private def wrapArray(lst: List[String]) = "\"" ++ joinList(lst) ++ "\""

    private def joinCsvRows: fs2.Pipe[F, String, String] =
      stream => stream.fold("") { case (str, it) => str ++ it ++ "\n" }

    private def storeCsv(
        stream: fs2.Stream[F, String],
        ob: ObjectId
    ): F[Unit] =
      stream
        .evalMap { str =>
          obs.put(ob, str.getBytes)
        }
        .compile
        .drain

    private def extractUsersToObs(ob: ObjectId): F[Unit] =
      storeCsv(
        cql"select * from local.users"
          .as[User]
          .select(session)
          .map(u => s"${u.id.value}, ${u.name.value}, ${u.email.value}")
          .through(joinCsvRows),
        ob
      )

    private def extractAdsToObs(ob: ObjectId): F[Unit] =
      storeCsv(
        cql"select * from local.advertisements"
          .as[Advertisement]
          .select(session)
          .map { a =>
            s"${a.id.value},${a.authorId.value},${a.title},${wrapArray(a.tags.toList.map(_.toString))}"
          }
          .through(joinCsvRows),
        ob
      )

    private def extractUserBoughtToObs(ob: ObjectId): F[Unit] =
      storeCsv(
        cql"select * from recs.user_bought"
          .as[(UserId, AdId)]
          .select(session)
          .map { case (a, b) =>
            s"${a.value},${b.value}"
          }
          .through(joinCsvRows),
        ob
      )

    private def extractUserDiscussedToObs(ob: ObjectId): F[Unit] =
      storeCsv(
        cql"select * from recs.user_bought"
          .as[(UserId, AdId)]
          .select(session)
          .map { case (a, b) =>
            s"${a.value},${b.value}"
          }
          .through(joinCsvRows),
        ob
      )

    private def extractUserCreatedToObs(ob: ObjectId): F[Unit] =
      storeCsv(
        cql"select * from recs.user_bought"
          .as[(UserId, AdId)]
          .select(session)
          .map { case (a, b) =>
            s"${a.value},${b.value}"
          }
          .through(joinCsvRows),
        ob
      )

  }

}
