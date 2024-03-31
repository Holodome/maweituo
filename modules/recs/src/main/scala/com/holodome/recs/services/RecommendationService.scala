package com.holodome.recs.services

import cats.{Applicative, MonadThrow}
import cats.data.{NonEmptyList, OptionT}
import cats.effect.std.Random
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.recs.algo.RecommendationAlgorithm
import com.holodome.recs.etl.RecETL
import com.holodome.recs.repositories.{RecRepository, TelemetryRepository}

trait RecommendationService[F[_]] {
  def getRecs(user: UserId, count: Int): F[List[AdId]]
  def learn(): F[Unit]
}

object RecommendationService {
  def make[F[_]: MonadThrow](
      algo: RecommendationAlgorithm[F],
      recRepo: RecRepository[F],
      etl: RecETL[F]
  )(implicit rng: Random[F]): RecommendationService[F] =
    new RecommendationServiceInterpreter(algo, recRepo, etl)

  private final class RecommendationServiceInterpreter[F[_]: MonadThrow](
      algo: RecommendationAlgorithm[F],
      recRepo: RecRepository[F],
      etl: RecETL[F]
  )(implicit rng: Random[F])
      extends RecommendationService[F] {
    override def getRecs(user: UserId, count: Int): F[List[AdId]] = {
      collaborativeRecs(user, count)
        .flatMap {
          case x if x.size < count =>
            contentRecs(user, count - x.size).map(_ ++ x)
          case x => Applicative[F].pure(x)
        }
        .map(_.toList)
    }

    private def collaborativeRecs(user: UserId, count: Int): F[Set[AdId]] =
      algo
        .getClosest(user)
        .flatMap { closest =>
          List(0 until count)
            .traverse(_ => rng.elementOf(closest))
            .flatMap(_.traverse(u => generateRecommendationBasedOnUser(u).value).map(_.flatten))
        }
        .map(_.toSet)

    private def generateRecommendationBasedOnUser(target: UserId): OptionT[F, AdId] = {
      OptionT
        .liftF(rng.betweenDouble(0, 1))
        .flatMap {
          case x if 0.0 <= x && x <= 0.1 => recRepo.getUserClicked(target)
          case x if 0.1 < x && x <= 0.4  => recRepo.getUserDiscussed(target)
          case _                         => recRepo.getUserBought(target)
        }
        .flatMap(ads => OptionT.liftF(rng.elementOf(ads)))
    }

    private def contentRecs(user: UserId, count: Int): F[Set[AdId]] =
      List(0 until count)
        .traverse(_ => genContentRec(user).value)
        .map(_.flatten)
        .map(_.toSet)

    private def genContentRec(user: UserId): OptionT[F, AdId] =
      recRepo.get(user).flatMap { weights =>
        val values = weights.values
          .foldLeft(NonEmptyList(0.0, Nil)) { case (sums, v) =>
            sums.head + v :: sums
          }
          .reverse
        val low  = values.head
        val high = values.last
        for {
          r <- OptionT.liftF(rng.betweenDouble(low, high))
          idx = values.takeWhile_(v => v < r).length
          tag <- recRepo.getTagByIdx(idx)
          ads <- recRepo.getAdsByTag(tag)
          ad  <- OptionT.liftF(rng.elementOf(ads))
        } yield ad
      }

    override def learn(): F[Unit] =
      etl.etlAll()
  }
}
