package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users.UserId

import java.time.Instant

trait FeedRepository[F[_]] {
  def getPersonalized(user: UserId, pag: Pagination): OptionT[F, List[AdId]]
  def getGlobal(pag: Pagination): F[List[AdId]]

  def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: Int): F[Unit]
  def addToGlobalFeed(ad: AdId, at: Instant): F[Unit]
}
