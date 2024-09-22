package com.holodome.cassandra.repositories

import cats.syntax.all._
import com.ringcentral.cassandra4io.CassandraSession
import cats.effect.kernel.Async
import com.holodome.domain.repositories.UserAdsRepository
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId
import com.ringcentral.cassandra4io.cql.CqlStringContext
import cats.data.OptionT

object CassandraUserAdsRepository {
  def make[F[_]: Async](session: CassandraSession[F]): UserAdsRepository[F] =
    new CassandraUserAdsRepository(session)
}

private final class CassandraUserAdsRepository[F[_]: Async](session: CassandraSession[F])
    extends UserAdsRepository[F] {

  override def create(userId: UserId, ad: AdId): F[Unit] =
    cql"update local.user_ads set ads = ads + {${ad.value}} where user_id = ${userId.value}"
      .execute(session)
      .void

  override def get(userId: UserId): OptionT[F, Set[AdId]] =
    OptionT(
      cql"select ads from local.user_ads where user_id = ${userId.value}"
        .as[Option[Set[AdId]]]
        .select(session)
        .head
        .compile
        .last
        .map {
          case Some(Some(x)) => x.some
          case Some(None)    => Set().some
          case None          => None
        }
    )
}
