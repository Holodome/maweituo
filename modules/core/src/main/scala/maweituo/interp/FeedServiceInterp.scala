package maweituo.interp
import cats.MonadThrow

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.{AdId, AdSortOrder, PaginatedAdsResponse}
import maweituo.domain.repos.RecsRepo
import maweituo.domain.services.{FeedService, IAMService}
import maweituo.domain.{Identity, PaginatedCollection, Pagination}
import maweituo.effects.TimeSource

import org.typelevel.log4cats.Logger

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      ads: AdRepo[F],
      recs: RecsRepo[F]
  )(using iam: IAMService[F]): FeedService[F] = new:

    def global(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] =
      ads.all(pag, order)

    def personalized(pag: Pagination)(using id: Identity): F[PaginatedCollection[AdId]] =
      recs.getClosestAds(id.id, pag)
