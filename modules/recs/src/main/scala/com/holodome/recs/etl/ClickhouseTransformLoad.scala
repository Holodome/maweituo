package com.holodome.recs.etl

import cats.syntax.all._
import cats.effect.kernel.MonadCancelThrow
import cats.NonEmptyParallel
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.OBSUrl
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations
import doobie.implicits.toSqlInterpolator
import doobie.util.transactor.Transactor
import doobie.implicits._
import com.holodome.recs.sql.codecs._
import org.typelevel.log4cats.Logger

import java.util.UUID
import com.holodome.ext.log4catsExt._

object ClickhouseTransformLoad {
  def make[F[_]: MonadCancelThrow: NonEmptyParallel: Logger](xa: Transactor[F]): RecETLLoader[F] =
    new ClickhouseTransformLoad(xa)
}

private final class ClickhouseTransformLoad[F[_]: MonadCancelThrow: NonEmptyParallel: Logger](
    xa: Transactor[F]
) extends RecETLLoader[F] {
  def load(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit] =
    Logger[F].bracketProtectInfo("Starting ETL load", "Finished ETL load", "ETL load aborted")(
      ClickhouseTransformLoadOperator(xa)(obs).load(locs)
    )
}

private final case class ClickhouseTransformLoadOperator[F[
    _
]: MonadCancelThrow: NonEmptyParallel: Logger](xa: Transactor[F])(obs: ObjectStorage[F]) {
  def load(locs: OBSSnapshotLocations): F[Unit] =
    for {
      _ <- Logger[F].protectInfo("Starting ETL users load", "Finished ETL users load")(
        loadUserCreated(obs.makeUrl(locs.user_created))
      )
      _ <- Logger[F].protectInfo("Starting ETL user bought load", "Finished ETL users bought load")(
        loadUserBought(obs.makeUrl(locs.user_bought))
      )
      _ <- Logger[F].protectInfo(
        "Starting ETL user discussed load",
        "Finished ETL user discussed load"
      )(loadUserDiscussed(obs.makeUrl(locs.user_discussed)))
      _ <- Logger[F].protectInfo("Starting ETL ad tags load", "Finished ETL ad tags load")(
        loadAdTags(obs.makeUrl(locs.ads))
      )
      _ <- Logger[F].protectInfo("Starting ETL tags load", "Finished ETL tags load")(loadTags)
      _ <- Logger[F].protectInfo(
        "Starting ETL weights calculation",
        "Finished ETL weights calculation"
      )(calculateWeights(obs.makeUrl(locs.users)))
    } yield ()

  private def calculateWeights(url: OBSUrl) = {
    def joinListsToSet[A](data: List[Set[A]]) =
      data.foldLeft(Set[A]())((s, lst) => s ++ lst)

    def tagsForAd(adId: UUID) =
      sql"select arrayJoin(tags) as tag from ad_tags where `id` = $adId"
        .query[String]
        .to[Set]
        .transact(xa)

    for {
      tags <- sql"select tag from tag_ads".query[String].to[List].transact(xa)
      uids <- sql"select `id` from s3(${url.value}, 'CaSWWithNames')"
        .query[UUID]
        .to[List]
        .transact(xa)
      _ <- uids.traverse_ { uid =>
        (
          sql"""select arrayJoin(ads) as ad from user_bought where `id` = $uid"""
            .query[UUID]
            .to[List]
            .transact(xa),
          sql"""select arrayJoin(ads) as ad from user_discussed where `id` = $uid"""
            .query[UUID]
            .to[List]
            .transact(xa),
          sql"""select arrayJoin(ads) as ad from user_created where `id` = $uid"""
            .query[UUID]
            .to[List]
            .transact(xa)
        ).parMapN {
          case (bought, discussed, created) => {
            (
              bought.traverse(tagsForAd).map(joinListsToSet),
              discussed.traverse(tagsForAd).map(joinListsToSet),
              created.traverse(tagsForAd).map(joinListsToSet)
            ).parMapN { case (boughtTags, createdTags, discussedTags) =>
              val tagsMap = tags
                .foldLeft(scala.collection.mutable.Map[String, Double]()) { case (m, t) =>
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
              val weightVector = tags.map(t => weightMap(t)).mkString(",")
              sql"""insert into user_weights (`id`, weights)
                      values ($uid, (select splitByChar(',', $weightVector)))""".update.run
                .transact(xa)
                .as(())
            }
          }
        }
      }
    } yield ()
  }

  private def loadAdTags(url: OBSUrl) =
    sql"""insert into ad_tags
            select `id`, splitByChar(',', tags) as tags from s3(${url.value}, 'CSWWithNames')
         """.update.run.transact(xa).void

  private def loadTags =
    sql"""with all_tags as (
              select unique arrayJoin(tags) as tag from ad_tags
            )
            insert into tag_ads
            select tag, groupArray(id) from all_tags
            join ad_tags t on has(t.tags, t.id)
         """.update.run.transact(xa).void

  private def loadUserBought(url: OBSUrl) =
    sql"""insert into user_bought
            (select `id`, groupArray(ad) as ads from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
             group by `id`
            )""".update.run.transact(xa).void

  private def loadUserDiscussed(url: OBSUrl) =
    sql"""insert into user_discussed
            (select `id`, groupArray(ad) as ads from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
             group by `id`
            )""".update.run.transact(xa).void

  private def loadUserCreated(url: OBSUrl) =
    sql"""insert into user_created
          select `id`, groupArray(`ad`) as `ads` from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
          group by `id`
            """.update.run.transact(xa).void
}
