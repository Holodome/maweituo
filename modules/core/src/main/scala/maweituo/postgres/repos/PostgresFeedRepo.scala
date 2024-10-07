package maweituo.postgres.repos
import scala.concurrent.duration.FiniteDuration

import cats.effect.Async

import maweituo.domain.ads.AdId
import maweituo.domain.pagination.Pagination
import maweituo.domain.repos.FeedRepo
import maweituo.domain.users.UserId

import doobie.Transactor

object PostgresFeedRepo:
  def make[F[_]: Async](xa: Transactor[F]): FeedRepo[F] = new:

    def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit] = Async[F].unit

    def getPersonalizedSize(user: UserId): F[Int] = Async[F].pure(0)

    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] = Async[F].pure(List())
