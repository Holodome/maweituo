package com.holodome.recs.repositories.cassandra

import cats.data.OptionT
import cats.effect.Async
import com.holodome.domain.{ads, users}
import com.holodome.ext.cassandra4io.typeMappers._
import com.ringcentral.cassandra4io.cql.Reads._
import com.holodome.cql.codecs._
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.users.UserId
import com.holodome.recs.domain.recommendations
import com.holodome.recs.repositories.RecRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraRecRepository {
  def make[F[_]: Async](session: CassandraSession[F]): RecRepository[F] =
    new CassandraRecRepository(session)
}

private final class CassandraRecRepository[F[_]: Async](session: CassandraSession[F])
    extends RecRepository[F] {

  override def get(userId: users.UserId): OptionT[F, recommendations.WeightVector] =
    OptionT(getQuery(userId).select(session).head.compile.last)
      .map(_.map(_.toFloat))
      .map(recommendations.WeightVector.apply)

  override def getUserClicked(user: users.UserId): OptionT[F, Set[ads.AdId]] =
    OptionT(getUserClickedQuery(user).select(session).head.compile.last)

  override def getUserBought(user: users.UserId): OptionT[F, Set[ads.AdId]] =
    OptionT(getUserBoughtQuery(user).select(session).head.compile.last)

  override def getUserDiscussed(user: users.UserId): OptionT[F, Set[ads.AdId]] = OptionT(
    getUserDiscussedQuery(user).select(session).head.compile.last
  )

  override def getTagByIdx(idx: Int): OptionT[F, ads.AdTag] =
    OptionT(getTagByIdxQuery(idx).select(session).head.compile.last)

  override def getAdsByTag(tag: ads.AdTag): OptionT[F, Set[ads.AdId]] =
    OptionT(getAdsByTagQuery(tag).select(session).head.compile.last)

  private def getQuery(userId: users.UserId) =
    cql"select weights from rec.user_weights where id = ${userId.value}".as[List[Double]]

  private def getUserClickedQuery(userId: UserId) =
    cql"select ads from rec.user_clicked_snapshot where id = ${userId.value}".as[Set[AdId]]

  private def getUserBoughtQuery(userId: UserId) =
    cql"select ads from rec.user_bought_snapshot where id = ${userId.value}".as[Set[AdId]]

  private def getUserDiscussedQuery(userId: UserId) =
    cql"select ads from rec.user_discussed_snapshot where id = ${userId.value}".as[Set[AdId]]

  private def getTagByIdxQuery(idx: Int) =
    cql"select tag from rec.tags where idx = $idx".as[AdTag]

  private def getAdsByTagQuery(tag: ads.AdTag) =
    cql"select ads from rec.ads_snapshot where tag = ${tag.value}".as[Set[AdId]]
}
