package maweituo.interp
import cats.MonadThrow

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.{AdId, AdSortOrder, PaginatedAdsResponse}
import maweituo.domain.pagination.Pagination
import maweituo.domain.repos.FeedRepo
import maweituo.domain.services.FeedService
import maweituo.domain.users.UserId
import maweituo.effects.TimeSource

import org.typelevel.log4cats.Logger

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      ads: AdRepo[F],
      feed: FeedRepo[F]
  ): FeedService[F] = new:
    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] =
      ???

    def getGlobal(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] = ads.all(pag, order)
    def getPersonalizedSize(user: UserId): F[Int]                               = feed.getPersonalizedSize(user)
