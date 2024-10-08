package maweituo.domain.services

import maweituo.domain.ads.{AdId, AdSortOrder, AdTag, PaginatedAdsResponse}
import maweituo.domain.{Identity, PaginatedCollection, Pagination}

trait FeedService[F[_]]:
  def personalized(pag: Pagination)(using Identity): F[PaginatedCollection[AdId]]

  def global(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse]
  def globalFiltered(pag: Pagination, order: AdSortOrder, allowedTags: List[AdTag]): F[PaginatedAdsResponse]
