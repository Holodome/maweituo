package maweituo
package postgres
package repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.syntax.*
export doobie.implicits.given
import doobie.postgres.implicits.given
import cats.NonEmptyParallel
import org.typelevel.log4cats.Logger

object PostgresRecsRepo:
  def make[F[_]: Async: NonEmptyParallel: Logger](xa: Transactor[F]): RecsRepo[F] = new:

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
      sql"select tag from tag_ads where ad_id = $adId::uuid"
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
        _     <- info"calulating tag weights"
        tagW  <- calculateTagWeights
        _     <- info"calculated ${tagW.knownSize} tag weights"
        _     <- info"calculating ad weights"
        adW   <- calculateAdWeights(tagW)
        _     <- info"calculated ${adW.knownSize} ad weights"
        _     <- info"calculating user weights"
        userW <- calculateUserWeights(adW)
        _     <- info"calculated ${userW.knownSize} user weights"
      yield userW -> adW

    private def resetDbWeights(userW: UserWeights, adW: AdWeights): F[Unit] =
      val ops =
        for
          _ <- sql"truncate user_weights".update.run
          _ <- sql"truncate ad_weights".update.run
          _ <-
            Update[(UserId, Weights)]("insert into user_weights(us, embedding) values (?::uuid, ?)")
              .updateMany(userW.map(x => x).toList)
          _ <-
            Update[(AdId, Weights)]("insert into ad_weights(ad, embedding) values (?::uuid, ?)")
              .updateMany(adW.map(x => x).toList)
        yield ()
      ops.transact(xa).void

    def learn: F[Unit] =
      for
        _ <- info"running learn"
        _ <- info"calculating new weights"
        w <- getNewWeights
        _ <- info"finished weights calculation"
        _ <- info"inserting new weights"
        _ <- resetDbWeights.tupled(w)
        _ <- info"finished weights insertion"
        _ <- info"finisher learning"
      yield ()
