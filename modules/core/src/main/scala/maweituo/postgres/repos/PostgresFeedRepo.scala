package maweituo.postgres.repos

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import maweituo.domain.ads.AdId
import maweituo.domain.pagination.Pagination
import maweituo.domain.repos.FeedRepo
import maweituo.domain.users.UserId
import maweituo.postgres.sql.codecs.given

import cats.effect.Async
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.given
import doobie.postgres.implicits.given

object PostgresFeedRepo:
  def make[F[_]: Async](xa: Transactor[F]): FeedRepo[F] = new:

    def getGlobal(pag: Pagination): F[List[AdId]] =
      sql"select ad_id from global_feed limit ${pag.limit} offset ${pag.offset}".query[AdId].to[List].transact(xa)

    def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: FiniteDuration): F[Unit] = Async[F].unit

    def addToGlobalFeed(ad: AdId, at: Instant): F[Unit] =
      sql"insert into global_feed(at, ad_id) values ($at, $ad)".update.run.transact(xa).void

    def getPersonalizedSize(user: UserId): F[Int] = Async[F].pure(0)

    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] = Async[F].pure(List())

    def getGlobalSize: F[Int] =
      sql"select count(*) from global_feed".query[Int].unique.transact(xa)
