package com.holodome.cassandra

import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cql.codecs._
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.repositories.TagRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraTagRepository {
  def make[F[_]: Async](session: CassandraSession[F]): TagRepository[F] =
    new CassandraTagRepository(session)
}

private final class CassandraTagRepository[F[_]: Async](session: CassandraSession[F])
    extends TagRepository[F] {

  override def getAllTags: F[List[AdTag]] =
    cql"select tag from local.tags".as[AdTag].select(session).compile.toList

  override def addTag(tag: AdTag): F[Unit] =
    cql"insert into local.tags (tag) values (${tag.str})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
      .execute(session)
      .void

  override def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
    cql"update local.tags set ads = ads + {$adId} where tag = ${tag.str}".execute(session).void

  override def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
    cql"update local.tags set ads = ads + {$adId} where tag = ${tag.str}".execute(session).void

  override def getAllAdsByTag(tag: AdTag): F[Set[AdId]] =
    cql"select ads from local.tags where tag = ${tag.str}"
      .as[Option[Set[AdId]]]
      .select(session)
      .head
      .compile
      .last
      .map(_.flatten)
      .map(_.getOrElse(Set.empty[AdId]))

}
