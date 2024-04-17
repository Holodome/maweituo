package com.holodome.recs.algo.clickhouse

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import com.holodome.domain.users.UserId
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.recs.algo.RecommendationAlgorithm
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID

private final class ClickhouseRecAlgo[F[_]: MonadCancelThrow](xa: Transactor[F])
    extends RecommendationAlgorithm[F] {

  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap[UUID](UUID.fromString)(_.toString)

  override def obsIngest(obs: ObjectStorage[F], id: ObjectId): F[Unit] =
    obsIngestQ(obs.makeUrl(id)).run.transact(xa).as(())

  private def obsIngestQ(obsUrl: String): Update0 =
    sql"""
         truncate table user_weights;
         insert into user_weights
          select * from s3($obsUrl, 'CSVWithNames');
         """.update

  override def getClosest(user: UserId, count: Int): F[List[UserId]] =
    getClosestQ(user, count)
      .to[List]
      .transact(xa)
      .map(_.map(UserId.apply))

  private def getClosestQ(user: UserId, count: Int): Query0[UUID] =
    sql"""select id from user_weights 
         order by L2Distance(weights, 
                            (select weights from user_weights where id = ${user.value})) 
         limit $count"""
      .query[UUID]
}
