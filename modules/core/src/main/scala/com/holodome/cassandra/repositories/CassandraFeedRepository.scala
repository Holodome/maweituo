package com.holodome.cassandra.repositories

import cats.effect.kernel.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.repositories.FeedRepository
import com.holodome.domain.users.UserId
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object CassandraFeedRepository {
  def make[F[_]: Async](session: CassandraSession[F]): FeedRepository[F] =
    new CassandraFeedRepository[F](session)
}

private final class CassandraFeedRepository[F[_]: Async](session: CassandraSession[F])
    extends FeedRepository[F] {

  override def getPersonalizedSize(user: UserId): F[Int] =
    cql"select cast(count(*) as int) from local.personalized_feed where user_id = ${user.value}"
      .as[Int]
      .select(session)
      .head
      .compile
      .last
      .map(_.getOrElse(0))

  override def getGlobalSize: F[Int] =
    cql"select cast(count(*) as int) from local.global_feed"
      .as[Int]
      .select(session)
      .head
      .compile
      .last
      .map(_.getOrElse(0))

  override def getPersonalized(
      user: UserId,
      pag: Pagination
  ): F[List[AdId]] =
    getPersonalizedQ(user, pag).select(session).compile.toList

  override def getGlobal(pag: Pagination): F[List[AdId]] =
    cql"select ad_id from local.global_feed"
      .as[AdId]
      .select(session)
      .drop(pag.lower)
      .take(pag.pageSize)
      .compile
      .toList

  override def setPersonalized(userId: UserId, ads: List[AdId], ttl: FiniteDuration): F[Unit] =
    ads
      .foldLeftM(0) { case (idx, id) =>
        setPersonalizedQ(userId, id, idx, ttl.toSeconds.toInt)
          .execute(session)
          .as(idx + 1)
      }
      .void

  override def addToGlobalFeed(ad: AdId, at: Instant): F[Unit] =
    cql"insert into local.global_feed (at, ad_id) values ($at, ${ad.value})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.ONE)
      )
      .execute(session)
      .void

  private def getPersonalizedQ(
      user: UserId,
      pag: Pagination
  ) =
    cql"select ad_id from local.personalized_feed where idx >= ${pag.lower} and idx < ${pag.upper} and user_id = ${user.value}"
      .as[AdId]

  private def setPersonalizedQ(user: UserId, ad: AdId, idx: Int, ttlSecs: Int) =
    cql"insert into local.personalized_feed (user_id, idx, ad_id) values (${user.value}, $idx, ${ad.value}) using ttl $ttlSecs"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.ONE)
      )

}
