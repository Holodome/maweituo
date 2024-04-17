package com.holodome.recs.etl

import cats.syntax.all._
import cats.effect.kernel.MonadCancelThrow
import cats.NonEmptyParallel
import com.holodome.infrastructure.ObjectStorage
import com.holodome.recs.domain.recommendations.OBSSnapshotLocations
import doobie.implicits.toSqlInterpolator
import doobie.util.transactor.Transactor
import doobie.implicits._
import com.holodome.recs.sql.codecs._

import java.util.UUID

class ClickhouseTransformLoad[F[_]: MonadCancelThrow: NonEmptyParallel](xa: Transactor[F]) {
  def load(locs: OBSSnapshotLocations, obs: ObjectStorage[F]): F[Unit] = Operator(obs).load(locs)

  private final case class Operator(obs: ObjectStorage[F]) {
    def load(locs: OBSSnapshotLocations): F[Unit] = ???

    private def calculateWeights(url: String) = {
      def joinListsToSet[A](data: List[Set[A]]) =
        data.foldLeft(Set[A]())((s, lst) => s ++ lst)

      def tagsForAd(adId: UUID) =
        sql"select arrayJoin(tags) as tag from ad_tags where id = $adId"
          .query[String]
          .to[Set]
          .transact(xa)

      for {
        tags <- sql"select tag from tag_ads".query[String].to[List].transact(xa)
        uids <- sql"select id from s3($url, 'CaSWWithNames')".query[UUID].to[List].transact(xa)
        _ <- uids.traverse_ { uid =>
          (
            sql"""select arrayJoin(ads) as ad from user_bought where id = $uid"""
              .query[UUID]
              .to[List]
              .transact(xa),
            sql"""select arrayJoin(ads) as ad from user_discussed where id = $uid"""
              .query[UUID]
              .to[List]
              .transact(xa),
            sql"""select arrayJoin(ads) as ad from user_created where id = $uid"""
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
                sql"""insert into user_weights (id, weights) 
                      values ($uid, (select splitByChar(',', $weightVector)))""".update.run
                  .transact(xa)
                  .as(())
              }
            }
          }
        }
      } yield ()
    }

    private def loadAdTags(url: String) =
      sql"""insert into ad_tags
            select id, splitByChar(',', tags) as tags from s3($url, 'CSWWithNames')
         """

    private def loadTags(url: String) =
      sql"""with all_tags as (
              select unique arrayJoin(tags) as tag from ad_tags
            )
            insert into tag_ads
            select tag, groupArray(id) from all_tags
            join ad_tags t on has(t.tags, t.id)
         """

    private def loadUserBought(url: String) =
      sql"""insert into user_bought
            (select id, groupArray(ad) as ads from s3($url, 'CSVWithNames')
             group by id 
            )"""

    private def loadUserDiscussed(url: String) =
      sql"""insert into user_discussed
            (select id, groupArray(ad) as ads from s3($url, 'CSVWithNames')
             group by id 
            )"""

    private def loadUserCreated(url: String) =
      sql"""insert into user_created
            (select id, groupArray(ad) as ads from s3($url, 'CSVWithNames')
             group by id 
            )"""
  }
}
