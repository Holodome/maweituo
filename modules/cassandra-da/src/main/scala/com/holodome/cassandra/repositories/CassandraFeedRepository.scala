package com.holodome.cassandra

import cats.effect.kernel.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.repositories.FeedRepository
import com.holodome.domain.users.UserId
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

import java.time.Instant

object CassandraFeedRepository {
  def make[F[_]: Async](session: CassandraSession[F]): FeedRepository[F] =
    new CassandraFeedRepository[F](session)
}

private final class CassandraFeedRepository[F[_]: Async](session: CassandraSession[F])
    extends FeedRepository[F] {

  override def getPersonalized(
      user: UserId,
      pag: Pagination
  ): F[List[AdId]] =
    getPersonalizedQ(user, pag).select(session).compile.toList

  override def getGlobal(pag: Pagination): F[List[AdId]] =
    getGlobalQ.select(session).drop(pag.lower).take(pag.pageSize).compile.toList

  override def setPersonalized(userId: UserId, ads: List[AdId], ttlSecs: Int): F[Unit] =
    ads
      .foldLeftM(0) { case (idx, id) =>
        setPersonalizedQ(userId, id, idx, ttlSecs).execute(session).as(idx + 1)
      }
      .void

  override def addToGlobalFeed(ad: AdId, at: Instant): F[Unit] =
    addToGlobalFeedQ(ad, at).execute(session).void

  private def getPersonalizedQ(
      user: UserId,
      pag: Pagination
  ) =
    cql"select ad_id from local.personalized_feed where idx >= ${pag.lower} and idx < ${pag.upper} and user_id = ${user.value}"
      .as[AdId]

  private def getGlobalQ =
    cql"select ad_id from local.global_feed"
      .as[AdId]

  private def setPersonalizedQ(user: UserId, ad: AdId, idx: Int, ttlSecs: Int) =
    cql"insert into local.personalized_feed (user_id, idx, ad_id) values (${user.value}, $idx, ${ad.value}) using ttl $ttlSecs"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.ONE)
      )

  private def addToGlobalFeedQ(ad: AdId, at: Instant) =
    cql"insert into local.global_feed (at, ad_id) values ($at, ${ad.value})".config(
      _.setConsistencyLevel(ConsistencyLevel.ONE)
    )
}
