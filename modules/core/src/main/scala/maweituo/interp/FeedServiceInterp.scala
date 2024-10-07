package maweituo.interp

import scala.concurrent.duration.DurationInt

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.ads.AdId
import maweituo.domain.ads.PaginatedAdsResponse
import maweituo.domain.ads.AdSortOrder
import maweituo.domain.pagination.Pagination
import maweituo.domain.repos.FeedRepo
import maweituo.domain.services.{FeedService, RecommendationService}
import maweituo.domain.users.UserId
import maweituo.effects.TimeSource

import org.typelevel.log4cats.Logger
import maweituo.domain.ads.repos.AdRepo

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      ads: AdRepo[F],
      feed: FeedRepo[F],
      recs: RecommendationService[F]
  ): FeedService[F] = new:
    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] =
      feed.getPersonalized(user, pag).flatMap {
        case List() =>
          for
            recs <- recs.getRecs(user, 100)
            _    <- feed.setPersonalized(user, recs, (60 * 60).seconds)
            r    <- feed.getPersonalized(user, pag)
            _    <- Logger[F].info(s"Generated feed for user $user")
          yield r
        case lst => lst.pure[F]
      }

    def getGlobal(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] = ads.all(pag, order)
    def getPersonalizedSize(user: UserId): F[Int]                               = feed.getPersonalizedSize(user)
