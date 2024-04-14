package com.holodome.services

import cats.{Applicative, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.FeedError
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users.UserId
import com.holodome.effects.TimeSource
import com.holodome.repositories.FeedRepository
import org.typelevel.log4cats.Logger

trait FeedService[F[_]] {
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobal(pag: Pagination): F[List[AdId]]
}

object FeedService {

  def make[F[_]: MonadThrow: Logger: TimeSource](
      repo: FeedRepository[F],
      recs: RecommendationService[F]
  ): FeedService[F] =
    new FeedServiceInterpreter(repo, recs)

  private final class FeedServiceInterpreter[F[_]: MonadThrow: Logger: TimeSource](
      repo: FeedRepository[F],
      recs: RecommendationService[F]
  ) extends FeedService[F] {
    override def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] =
      repo.getPersonalized(user, pag).value.flatMap {
        case Some(ads) => Applicative[F].pure(ads)
        case None =>
          for {
            recs <- recs.getRecs(user, 100)
            _    <- repo.setPersonalized(user, recs, 60 * 60)
            r    <- repo.getPersonalized(user, pag).getOrRaise(FeedError())
            _    <- Logger[F].info(s"Generated feed for user $user")
          } yield r
      }

    override def getGlobal(pag: Pagination): F[List[AdId]] =
      repo.getGlobal(pag)
  }
}
