package maweituo.interp
import cats.MonadThrow
import cats.data.NonEmptyList
import cats.syntax.all.*

import maweituo.domain.ads.repos.AdSearchRepo
import maweituo.domain.ads.{AdId, AdSortOrder, AdTag, PaginatedAdsResponse}
import maweituo.domain.repos.RecsRepo
import maweituo.domain.services.{FeedService, IAMService}
import maweituo.domain.{Identity, PaginatedCollection, Pagination}
import maweituo.effects.TimeSource

import org.typelevel.log4cats.Logger

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      adSearch: AdSearchRepo[F],
      recs: RecsRepo[F]
  )(using iam: IAMService[F]): FeedService[F] = new:

    def global(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] =
      adSearch.all(pag, order).map {
        col => PaginatedAdsResponse(col, order)
      }

    def globalFiltered(pag: Pagination, order: AdSortOrder, allowedTags: List[AdTag]): F[PaginatedAdsResponse] =
      NonEmptyList.fromList(allowedTags) match
        case None => PaginatedAdsResponse.empty.pure[F] // TOOD: This is an error
        case Some(value) => adSearch
            .allFiltered(pag, order, value)
            .map { col => PaginatedAdsResponse(col, order) }

    def personalized(pag: Pagination)(using id: Identity): F[PaginatedCollection[AdId]] =
      recs.getClosestAds(id.id, pag)
