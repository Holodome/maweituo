package com.holodome.recs.etl

import cats.effect.Async
import cats.syntax.all._
import com.holodome.ext.log4catsExt._
import cats.NonEmptyParallel
import com.holodome.domain.ads.{AdId, AdTag, AdTitle}
import com.holodome.domain.users.{User, UserId}
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.ringcentral.cassandra4io.CassandraSession
import com.holodome.cql.codecs._
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.OBSId
import org.typelevel.log4cats.Logger

object CassandraExtract {
  def make[F[_]: Async: NonEmptyParallel: Logger](
      session: CassandraSession[F]
  ): RecETLExtractor[F] =
    new CassandraExtract(session)
}

private final class CassandraExtract[F[_]: Async: NonEmptyParallel: Logger](
    session: CassandraSession[F]
) extends RecETLExtractor[F] {

  override def extract(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit] =
    Logger[F]
      .bracketProtectInfo(
        "Starting ETL extraction",
        "Finished ETL extraction",
        "ETL extraction aborted"
      )(CassandraExtractOperator(session, obs).extract(locs))
}

private final case class CassandraExtractOperator[F[_]: Async: NonEmptyParallel: Logger](
    session: CassandraSession[F],
    obs: ObjectStorage[F]
) {
  def extract(locs: OBSSnapshotLocations): F[Unit] =
    (
      Logger[F].protectInfo("Starting ETL extract users", "Finished ETL extract users")(
        extractUsersToObs(locs.users)
      ),
      Logger[F].protectInfo("Starting ETL extract ads", "Finished ETL extract ads")(
        extractAdsToObs(locs.ads)
      ),
      Logger[F].protectInfo(
        "Starting ETL extract user bought",
        "Finished ETL extract user bought"
      )(extractUserBoughtToObs(locs.user_bought)),
      Logger[F].protectInfo(
        "Starting ETL extract user created",
        "Finished ETL extract user created"
      )(extractUserCreatedToObs(locs.user_created)),
      Logger[F].protectInfo(
        "Starting ETL extract user discussed",
        "Finished ETL extract user discussed"
      )(extractUserDiscussedToObs(locs.user_discussed))
    ).parMapN((_, _, _, _, _) => ())

  private def joinList(lst: List[String])  = lst.mkString(",")
  private def wrapArray(lst: List[String]) = "\"" ++ joinList(lst) ++ "\""

  private def joinCsvRows: fs2.Pipe[F, String, String] =
    stream => stream.fold("")((str, it) => str ++ it ++ "\n")

  private def storeCsv(
      stream: fs2.Stream[F, String],
      ob: OBSId
  ): F[Unit] =
    stream
      .evalMap { str =>
        obs.put(ob, str.getBytes)
      }
      .compile
      .drain

  private def extractUsersToObs(ob: OBSId): F[Unit] =
    storeCsv(
      cql"select * from local.users"
        .as[User]
        .select(session)
        .map(u => s"${u.id.value},${u.name.value},${u.email.value}")
        .through(joinCsvRows)
        .map("id,name,email\n" ++ _),
      ob
    )

  private def extractAdsToObs(ob: OBSId): F[Unit] =
    storeCsv(
      cql"select id, author_id, title, tags from local.advertisements"
        .as[(AdId, UserId, AdTitle, Option[Set[AdTag]])]
        .select(session)
        .map { a =>
          s"${a._1.value},${a._2.value},${a._3},${wrapArray(a._4.fold(List[String]())(s => s.toList.map(_.value)))}"
        }
        .through(joinCsvRows)
        .map("id,author,title,tags\n" ++ _),
      ob
    )

  private def extractUserBoughtToObs(ob: OBSId): F[Unit] =
    storeCsv(
      cql"select * from recs.user_bought"
        .as[(UserId, AdId)]
        .select(session)
        .map { case (a, b) =>
          s"${a.value},${b.value}"
        }
        .through(joinCsvRows)
        .map("id,ad\n" ++ _),
      ob
    )

  private def extractUserDiscussedToObs(ob: OBSId): F[Unit] =
    storeCsv(
      cql"select * from recs.user_discussed"
        .as[(UserId, AdId)]
        .select(session)
        .map { case (a, b) =>
          s"${a.value},${b.value}"
        }
        .through(joinCsvRows)
        .map("id,ad\n" ++ _),
      ob
    )

  private def extractUserCreatedToObs(ob: OBSId): F[Unit] =
    storeCsv(
      cql"select * from recs.user_created"
        .as[(UserId, AdId)]
        .select(session)
        .map { case (a, b) =>
          s"${a.value},${b.value}"
        }
        .through(joinCsvRows)
        .map("id,ad\n" ++ _),
      ob
    )

}
