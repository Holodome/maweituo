package maweituo.domain.services

import maweituo.domain.ads.{AdId, AdSortOrder, PaginatedAdsResponse}
import maweituo.domain.pagination.Pagination
import maweituo.domain.users.UserId

trait FeedService[F[_]]:
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobal(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse]
  def getPersonalizedSize(user: UserId): F[Int]
