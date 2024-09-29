package com.holodome.domain.repositories

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users.UserId

trait FeedRepository[F[_]]:
  def getPersonalizedSize(user: UserId): F[Int]
  def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]]
  def getGlobalSize: F[Int]
  def getGlobal(pag: Pagination): F[List[AdId]]
  def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit]
  def addToGlobalFeed(ad: AdId, at: Instant): F[Unit]
