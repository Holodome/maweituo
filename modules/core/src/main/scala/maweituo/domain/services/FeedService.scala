package maweituo.domain.services

import maweituo.domain.ads.{AdId, AdSortOrder, PaginatedAdsResponse}
import maweituo.domain.{Identity, PaginatedCollection, Pagination}

trait FeedService[F[_]]:
  def personalized(pag: Pagination)(using Identity): F[PaginatedCollection[AdId]]
  def global(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse]
