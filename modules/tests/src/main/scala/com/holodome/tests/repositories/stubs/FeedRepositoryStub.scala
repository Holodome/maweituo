package com.holodome.tests.repositories.stubs

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.repositories.FeedRepository
import com.holodome.domain.users.UserId

import cats.effect.IO
import cats.syntax.all.*

final class FeedRepositoryStub extends FeedRepository[IO]:

  override def getPersonalizedSize(user: UserId): IO[Int] = IO.pure(0)

  override def getGlobalSize: IO[Int] = IO.pure(0)

  override def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): IO[Unit] =
    IO.unit

  override def getPersonalized(
      user: UserId,
      pag: Pagination
  ): IO[List[AdId]] = List.empty[AdId].pure[IO]

  override def getGlobal(pag: Pagination): IO[List[AdId]] = List.empty[AdId].pure[IO]

  override def addToGlobalFeed(ad: AdId, at: Instant): IO[Unit] = IO.unit
