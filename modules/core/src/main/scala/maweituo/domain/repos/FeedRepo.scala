package maweituo.domain.repos
import scala.concurrent.duration.FiniteDuration

import maweituo.domain.ads.AdId
import maweituo.domain.pagination.Pagination
import maweituo.domain.users.UserId

trait FeedRepo[F[_]]:
  def getPersonalizedSize(user: UserId): F[Int]
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit]
