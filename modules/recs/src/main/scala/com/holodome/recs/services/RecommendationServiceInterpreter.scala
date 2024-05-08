package com.holodome.recs.services

import cats.MonadThrow
import cats.data.{NonEmptyList, OptionT}
import cats.effect.std.Random
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.repositories.RecRepository
import com.holodome.domain.services.RecommendationService
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.GenObjectStorageId
import com.holodome.recs.etl.RecETL
import cats.Applicative
import com.holodome.effects.Background

object RecommendationServiceInterpreter {
  def make[F[_]: MonadThrow: GenObjectStorageId: Background](
      recRepo: RecRepository[F],
      etl: RecETL[F]
  )(implicit rng: Random[F]): RecommendationService[F] =
    new RecommendationServiceInterpreter(recRepo, etl)
}

private final class RecommendationServiceInterpreter[F[
    _
]: MonadThrow: GenObjectStorageId: Background](
    recRepo: RecRepository[F],
    etl: RecETL[F]
)(implicit rng: Random[F])
    extends RecommendationService[F] {

  override def getRecs(user: UserId, count: Int): F[List[AdId]] =
    recRepo.userIsInRecs(user).flatMap {
      case true =>
        collaborativeRecs(user, count)
          .flatMap {
            case x if x.size < count =>
              contentRecs(user, count - x.size).map(_ ++ x)
            case x => x.pure[F]
          }
          .map(_.toList)
      case false => Applicative[F].pure(List[AdId]()) <* Background[F].schedule(learn)
    }

  private def collaborativeRecs(user: UserId, count: Int): F[Set[AdId]] = for {
    closest <- recRepo.getClosest(user, 10)
    s <- List(0 until count)
      .traverse(_ => rng.elementOf(closest))
      .flatMap(_.traverse(u => generateRecommendationBasedOnUser(u).value).map(_.flatten))
      .map(_.toSet)
  } yield s

  private def generateRecommendationBasedOnUser(target: UserId): OptionT[F, AdId] =
    OptionT
      .liftF(rng.nextDouble)
      .flatMap {
        case x if 0.0 <= x && x <= 0.1 => recRepo.getUserCreated(target)
        case x if 0.1 < x && x <= 0.4  => recRepo.getUserDiscussed(target)
        case _                         => recRepo.getUserBought(target)
      }
      .flatMap(ads => OptionT.liftF(rng.elementOf(ads)))

  private def contentRecs(user: UserId, count: Int): F[Set[AdId]] =
    List(0 until count)
      .traverse(_ => genContentRec(user).value)
      .map(_.flatten.toSet)

  private def genContentRec(user: UserId): OptionT[F, AdId] = for {
    weights <- recRepo.get(user)
    values = weights.values
      .foldLeft(NonEmptyList(0.0, Nil)) { case (sums, v) =>
        sums.head + v :: sums
      }
      .reverse
    low  = values.head
    high = values.last
    r <-
      if (low < high) { OptionT.liftF(rng.betweenDouble(low, high)) }
      else { OptionT.none }
    idx = values.takeWhile_(v => v < r).length
    tag <- recRepo.getTagByIdx(idx)
    ads <- recRepo.getAdsByTag(tag)
    ad  <- OptionT.liftF(rng.elementOf(ads))
  } yield ad

  override def learn: F[Unit] =
    etl.run

}
