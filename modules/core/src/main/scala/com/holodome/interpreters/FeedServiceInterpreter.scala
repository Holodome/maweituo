package com.holodome.interpreters

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.repositories.FeedRepository
import com.holodome.domain.services.{FeedService, RecommendationService}
import com.holodome.domain.users.UserId
import com.holodome.effects.TimeSource
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object FeedServiceInterpreter {

  def make[F[_]: MonadThrow: Logger: TimeSource](
      repo: FeedRepository[F],
      recs: RecommendationService[F]
  ): FeedService[F] =
    new FeedServiceInterpreter(repo, recs)

}

private final class FeedServiceInterpreter[F[_]: MonadThrow: Logger: TimeSource](
    repo: FeedRepository[F],
    recs: RecommendationService[F]
) extends FeedService[F] {
  override def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] =
    repo.getPersonalized(user, pag).flatMap {
      case List() =>
        for {
          recs <- recs.getRecs(user, 100)
          _    <- repo.setPersonalized(user, recs, (60 * 60).seconds)
          r    <- repo.getPersonalized(user, pag)
          _    <- Logger[F].info(s"Generated feed for user $user")
        } yield r
      case lst => lst.pure[F]
    }

  override def getGlobal(pag: Pagination): F[List[AdId]] = repo.getGlobal(pag)
  override def getGlobalSize: F[Int]                     = repo.getGlobalSize
  override def getPersonalizedSize(user: UserId): F[Int] = repo.getPersonalizedSize(user)
}
