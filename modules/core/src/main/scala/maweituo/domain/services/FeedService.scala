package maweituo.domain.services

import maweituo.domain.ads.AdId
import maweituo.domain.pagination.Pagination
import maweituo.domain.users.UserId

trait FeedService[F[_]]:
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobal(pag: Pagination): F[List[AdId]]
  def getPersonalizedSize(user: UserId): F[Int]
  def getGlobalSize: F[Int]
