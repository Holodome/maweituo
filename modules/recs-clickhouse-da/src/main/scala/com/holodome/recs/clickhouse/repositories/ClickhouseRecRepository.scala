package com.holodome.recs.clickhouse.repositories

import cats.data.OptionT
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.recs.domain.recommendations.WeightVector
import com.holodome.recs.domain.repositories.RecRepository
import com.holodome.domain.users.UserId
import com.holodome.recs.clickhouse.sql.codecs._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID
import com.holodome.utils.EncodeRF

object ClickhouseRecRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): RecRepository[F] =
    new ClickhouseRecRepository(xa)
}

private final class ClickhouseRecRepository[F[_]: MonadCancelThrow](xa: Transactor[F])
    extends RecRepository[F] {

  override def userIsInRecs(id: UserId): F[Boolean] =
    sql"select (${id.value} in (select id from user_weights))"
      .query[Boolean]
      .option
      .transact(xa)
      .map(_.getOrElse(false))

  override def get(userId: UserId): OptionT[F, WeightVector] =
    OptionT(getQuery(userId).option.transact(xa))
      .map(WeightVector.apply)

  override def getUserCreated(user: UserId): OptionT[F, Set[AdId]] =
    OptionT(getUserCreatedQuery(user).option.transact(xa)).map(_.toSet.map(AdId.apply))

  override def getUserBought(user: UserId): OptionT[F, Set[AdId]] =
    OptionT(getUserBoughtQuery(user).option.transact(xa)).map(_.toSet.map(AdId.apply))

  override def getUserDiscussed(user: UserId): OptionT[F, Set[AdId]] =
    OptionT(getUserDiscussedQuery(user).option.transact(xa)).map(_.toSet.map(AdId.apply))

  override def getTagByIdx(idx: Int): OptionT[F, AdTag] =
    OptionT(getTagByIdxQuery(idx).option.transact(xa)).flatMap { str => 
      OptionT.liftF(EncodeRF[F, String, AdTag].encodeRF(str))
    }

  override def getAdsByTag(tag: AdTag): OptionT[F, Set[AdId]] =
    OptionT(getAdsByTagQuery(tag).option.transact(xa)).map(_.toSet.map(AdId.apply))

  private def getQuery(userId: UserId) =
    sql"select weights from user_weights where id = ${userId.value}"
      .query[List[Float]]

  private def getUserCreatedQuery(userId: UserId) =
    sql"select ads from user_created final where id = ${userId.value}".query[List[UUID]]

  private def getUserBoughtQuery(userId: UserId) =
    sql"select ads from user_bought final where id = ${userId.value}".query[List[UUID]]

  private def getUserDiscussedQuery(userId: UserId) =
    sql"select ads from user_discussed final where id = ${userId.value}".query[List[UUID]]

  private def getTagByIdxQuery(idx: Int) =
    sql"select tag from tag_ads final limit 1 offset $idx".query[String]

  private def getAdsByTagQuery(tag: AdTag) =
    sql"select ads from tag_ads final where tag = ${tag.str}".query[List[UUID]]

  override def getClosest(user: UserId, count: Int): F[List[UserId]] =
    getClosestQ(user, count)
      .to[List]
      .transact(xa)
      .map(_.map(UserId.apply))

  private def getClosestQ(user: UserId, count: Int): Query0[UUID] =
    sql"""select id from user_weights
          final
          order by L2Distance(weights,
                            (select weights from user_weights where id = ${user.value}))
          limit $count"""
      .query[UUID]
}
