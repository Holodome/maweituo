package maweituo.domain.repos

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import maweituo.domain.ads.AdId
import maweituo.domain.pagination.Pagination
import maweituo.domain.users.UserId

trait FeedRepository[F[_]]:
  def getPersonalizedSize(user: UserId): F[Int]
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobalSize: F[Int]
  def getGlobal(pag: Pagination): F[List[AdId]]
  def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit]
  def addToGlobalFeed(ad: AdId, at: Instant): F[Unit]
