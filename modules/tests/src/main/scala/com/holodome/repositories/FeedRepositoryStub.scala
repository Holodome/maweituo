package com.holodome.repositories

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.users.UserId

import java.time.Instant

final class FeedRepositoryStub extends FeedRepository[IO] {
  override def getPersonalized(
      user: UserId,
      pag: Pagination
  ): IO[List[AdId]] = List.empty[AdId].pure[IO]

  override def getGlobal(pag: Pagination): IO[List[AdId]] = List.empty[AdId].pure[IO]

  override def setPersonalized(
      userId: UserId,
      ads: List[AdId],
      ttlSecs: Int
  ): IO[Unit] = IO.unit

  override def addToGlobalFeed(ad: AdId, at: Instant): IO[Unit] = IO.unit

}
