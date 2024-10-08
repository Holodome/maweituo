package maweituo.postgres.repos

import cats.effect.kernel.Async
import cats.syntax.all.*

import maweituo.domain.ads.AdId
import maweituo.domain.repos.RecsRepo
import maweituo.domain.users.*
import maweituo.postgres.sql.codecs.given

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
export doobie.implicits.given
import doobie.postgres.implicits.given
import cats.NonEmptyParallel
import cats.data.NonEmptyList
import maweituo.domain.ads.AdTag

object PostgresRecsRepo:
  def make[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): RecsRepo[F] = new:

    def getClosestAds(user: UserId, count: Int): F[List[AdId]] =
      sql"""select aw.ad from ad_weights aw
            order by aw.embedding <=> (select embedding from user_weights where us = $user::uuid)
            limit $count
      """.query[AdId].to[List].transact(xa)

    type Weights     = Vector[Float]
    type UserWeights = Map[UserId, Vector[Float]]
    type AdWeights   = Map[AdId, Vector[Float]]
    type TagWeights  = Map[AdTag, Vector[Float]]

    private val width                   = 16
    private val defaultWeights: Weights = new Array[Float](width).toVector

    private def weightsAdd(a: Weights, b: Weights): Weights =
      for (ai, bi) <- a zip b yield ai + bi

    private def calculateSingleUserWeights(adWeights: AdWeights, userId: UserId): F[Weights] =
      val userCreated =
        sql"select ad from user_created where us = $userId::uuid".query[AdId].to[List].transact(xa)
      val userViewed =
        sql"select ad from user_viewed where us = $userId::uuid".query[AdId].to[List].transact(xa)
      val userDiscussed =
        sql"select ad from user_discussed where us = $userId::uuid".query[AdId].to[List].transact(xa)
      val userBought =
        sql"select ad from user_bought where us = $userId::uuid".query[AdId].to[List].transact(xa)
      (userCreated, userViewed, userDiscussed, userBought).parMapN {
        (userCreated, userViewed, userDiscussed, userBought) =>
          val a = userCreated.map(adWeights.getOrElse(_, defaultWeights))
          val b = userViewed.map(adWeights.getOrElse(_, defaultWeights))
          val c = userDiscussed.map(adWeights.getOrElse(_, defaultWeights))
          val d = userBought.map(adWeights.getOrElse(_, defaultWeights))
          NonEmptyList.of(
            a,
            b,
            c,
            d
          ).map(_.foldLeft(defaultWeights)(weightsAdd))
            .reduceLeft(weightsAdd)
      }

    private def calculateUserWeights(adWeights: AdWeights): F[UserWeights] =
      sql"select id from users"
        .query[UserId]
        .to[List]
        .transact(xa)
        .flatMap { users =>
          users
            .traverse(userId => calculateSingleUserWeights(adWeights, userId).map(vec => (userId, vec)))
            .map(Map[UserId, Vector[Float]]() ++ _)
        }

    private def calculateTagWeights: F[TagWeights] =
      sql"select tag from tag_ads"
        .query[AdTag]
        .to[List]
        .transact(xa)
        .map { tags =>
          val weights =
            for (t, i) <- tags.zipWithIndex yield
              val w = if i < width then defaultWeights.updated(i, 1.0f) else defaultWeights
              (t, w)
          Map[AdTag, Vector[Float]]() ++ weights
        }

    private def calculateSingleAdWeight(tagWeights: TagWeights, adId: AdId): F[Vector[Float]] =
      sql"select tag from tag_ads where ad_id == $adId::uuid"
        .query[AdTag]
        .to[List]
        .transact(xa)
        .map {
          _.map(tagWeights.get).flatten.foldLeft(defaultWeights)(weightsAdd)
        }

    private def calculateAdWeights(tagWeights: TagWeights): F[AdWeights] =
      sql"select id from advertisements"
        .query[AdId].to[List].transact(xa).flatMap { ads =>
          ads.traverse(ad => calculateSingleAdWeight(tagWeights, ad).map(w => (ad, w)))
            .map(Map[AdId, Vector[Float]]() ++ _)
        }

    private def getNewWeights: F[(UserWeights, AdWeights)] =
      for
        tagW  <- calculateTagWeights
        adW   <- calculateAdWeights(tagW)
        userW <- calculateUserWeights(adW)
      yield userW -> adW

    def learn: F[Unit] =
      getNewWeights.flatMap { (userW, adW) =>
        val ops =
          for
            _ <- sql"truncate user_weights".update.run
            _ <- sql"truncate ad_weights".update.run
            _ <-
              Update[(UserId, Weights)]("insert into user_weights(us, embedding) values (?, ?)")
                .updateMany(userW.map(x => x).toList)
            _ <-
              Update[(AdId, Weights)]("insert into ad_weights(ad, embedding) values (?, ?)")
                .updateMany(adW.map(x => x).toList)
          yield ()
        ops.transact(xa).void
      }
