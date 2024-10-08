package maweituo.domain.repos

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId
import maweituo.domain.{PaginatedCollection, Pagination}

trait RecsRepo[F[_]]:
  def getClosestAds(user: UserId, pag: Pagination): F[PaginatedCollection[AdId]]
  def learn: F[Unit]
