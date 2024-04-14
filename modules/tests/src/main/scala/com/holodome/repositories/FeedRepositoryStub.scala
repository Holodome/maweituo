package com.holodome.repositories

import cats.data.OptionT
import cats.effect.IO
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.{ads, users}

import java.time.Instant

final class FeedRepositoryStub extends FeedRepository[IO] {
  override def getPersonalized(
      user: users.UserId,
      pag: Pagination
  ): OptionT[IO, List[AdId]] = OptionT.none

  override def getGlobal(pag: Pagination): IO[List[ads.AdId]] = IO.pure(List())

  override def setPersonalized(
      userId: users.UserId,
      ads: List[AdId],
      ttlSecs: Int
  ): IO[Unit] = IO.unit

  override def addToGlobalFeed(ad: ads.AdId, at: Instant): IO[Unit] = IO.unit

}
