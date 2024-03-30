package com.holodome.services

import cats.syntax.all._
import cats.{Applicative, Functor, Monad, MonadThrow, Traverse}
import cats.effect.std.Random
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.holodome.rec.RecommendationAlgorithm
import com.holodome.repositories.TelemetryRepository

trait RecommendationService[F[_]] {
  def getRecs(user: UserId, count: Int): F[List[AdId]]
}

object RecommendationService {
  private final class RecommendationServiceInterpreter[F[_]: MonadThrow](
      closestUsersFinder: RecommendationAlgorithm[F],
      telemetryRepository: TelemetryRepository[F],
      rng: Random[F]
  ) extends RecommendationService[F] {
    override def getRecs(user: UserId, count: Int): F[List[AdId]] = {
      collaborativeRecs(user, count)
        .flatMap {
          case x if x.size < count =>
            contentRecs(user, count - x.size).map(_ ++ x)
          case x => Applicative[F].pure(x)
        }
        .map(_.toList)
    }

    private def contentRecs(user: UserId, count: Int): F[Set[AdId]] = Applicative[F].pure(Set())

    private def collaborativeRecs(user: UserId, count: Int): F[Set[AdId]] =
      closestUsersFinder
        .get(user)
        .flatMap { closest =>
          List(0 until count)
            .traverse(_ => rng.elementOf(closest))
            .flatMap(_.traverse(generateRecommendationBasedOnUser))
        }
        .map(_.toSet)

    private def generateRecommendationBasedOnUser(target: UserId): F[AdId] = {
      rng
        .betweenDouble(0, 1)
        .flatMap {
          case x if 0.0 <= x && x <= 0.1 => telemetryRepository.getUserClicked(target)
          case x if 0.1 <= x && x <= 0.4 => telemetryRepository.getUserDiscussed(target)
          case _                         => telemetryRepository.getUserBought(target)
        }
        .flatMap { ads =>
          rng.elementOf(ads)
        }
    }
  }
}
