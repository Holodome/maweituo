package com.holodome.recs.etl

import cats.Parallel
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import com.holodome.ext.log4catsExt._
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.OBSUrl
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations
import com.holodome.recs.sql.codecs._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import java.util.UUID

object ClickhouseTransformLoad {
  def make[F[_]: MonadCancelThrow: Parallel: Logger](xa: Transactor[F]): RecETLLoader[F] =
    new ClickhouseTransformLoad(xa)
}

private final class ClickhouseTransformLoad[F[_]: MonadCancelThrow: Parallel: Logger](
    xa: Transactor[F]
) extends RecETLLoader[F] {
  def load(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit] =
    Logger[F].bracketProtectInfo("Starting ETL load", "Finished ETL load", "ETL load aborted")(
      ClickhouseTransformLoadOperator(xa)(obs).load(locs)
    )
}

private final case class ClickhouseTransformLoadOperator[F[
    _
]: MonadCancelThrow: Parallel: Logger](xa: Transactor[F])(obs: ObjectStorage[F]) {
  def load(locs: OBSSnapshotLocations): F[Unit] = for {
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

  private def joinListsToSet[A](data: List[Set[A]]): Set[A] =
    data.foldMap(a => a)

  private def tagsForAd(adId: UUID) =
    sql"select arrayJoin(tags) as tag from ad_tags where `id` = $adId"
      .query[String]
      .to[Set]
      .transact(xa)

  private def calculateWeights(url: OBSUrl): F[Unit] = for {
    tags <- sql"select tag from tag_ads".query[String].to[List].transact(xa)
    uids <-
      sql"select `id` from s3(${url.value}, 'CSVWithNames', '`id` UUID, `name` String, `email` String')"
        .query[UUID]
        .to[List]
        .transact(xa)
    _ <- uids.parTraverse_ { uid =>
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
      ).flatMapN { case (bought, discussed, created) =>
        (
          bought.traverse(tagsForAd).map(joinListsToSet),
          discussed.traverse(tagsForAd).map(joinListsToSet),
          created.traverse(tagsForAd).map(joinListsToSet)
        ).parMapN { case (boughtTags, discussedTags, createdTags) =>
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
            .void
        }
      }
    }
  } yield ()

  private def loadAdTags(url: OBSUrl) =
    sql"""insert into ad_tags
            select `id`, splitByChar(',', `tags`) as `tags`
            from s3(${url.value}, 'CSVWithNames', '`id` UUID, `tags` String')
         """.update.run.transact(xa).void

  private def loadTags =
    sql"""insert into tag_ads
          select `tag`, groupArray(`id`) as `ads` from (
            select distinct arrayJoin(`tags`) as `tag` from ad_tags
          ) a, ad_tags t
          where has(t.`tags`, `tag`)
          group by `tag`""".update.run.transact(xa).void

  private def loadUserBought(url: OBSUrl) =
    sql"""insert into user_bought
            select `id`, groupArray(ad) as ads
            from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
            group by `id`""".update.run.transact(xa).void

  private def loadUserDiscussed(url: OBSUrl) =
    sql"""insert into user_discussed
            select `id`, groupArray(ad) as ads
            from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
            group by `id`""".update.run.transact(xa).void

  private def loadUserCreated(url: OBSUrl) =
    sql"""insert into user_created
            select `id`, groupArray(`ad`) as `ads`
            from s3(${url.value}, 'CSVWithNames', '`id` UUID, `ad` String')
            group by `id`""".update.run.transact(xa).void
}
