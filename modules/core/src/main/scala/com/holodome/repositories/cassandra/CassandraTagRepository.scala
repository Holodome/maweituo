package com.holodome.repositories.cassandra

import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.repositories.TagRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.holodome.cql.codecs._
import com.holodome.domain.ads.{AdId, AdTag}

object CassandraTagRepository {
  def make[F[_]: Async](session: CassandraSession[F]): TagRepository[F] =
    new CassandraTagRepository(session)
}

private final class CassandraTagRepository[F[_]: Async](session: CassandraSession[F])
    extends TagRepository[F] {

  override def getAllTags: F[List[AdTag]] =
    getAllTagsQ.select(session).compile.toList

  override def addTag(tag: AdTag): F[Unit] =
    addTagQ(tag).execute(session).void

  override def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
    addTagToAdQ(adId, tag).execute(session).void

  override def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
    removeTagFromAdQ(adId, tag).execute(session).void

  override def getAllAdsByTag(tag: AdTag): F[Set[AdId]] =
    getAllAdsByTagQ(tag)
      .select(session)
      .head
      .compile
      .last
      .map(_.flatten)
      .map(_.getOrElse(Set.empty[AdId]))

  private def getAllTagsQ =
    cql"select tag from local.tags".as[AdTag]

  private def addTagQ(tag: AdTag) =
    cql"insert into local.tags (tag) values ($tag)".config(
      _.setConsistencyLevel(ConsistencyLevel.QUORUM)
    )

  private def addTagToAdQ(adId: AdId, tag: AdTag) =
    cql"update local.tags set ads = ads + {$tag} where id = $adId"

  private def removeTagFromAdQ(adId: AdId, tag: AdTag) =
    cql"update local.tags set ads = ads + {$tag} where id = $adId"

  private def getAllAdsByTagQ(tag: AdTag) =
    cql"select ads from local.tags where tag = $tag".as[Option[Set[AdId]]]

}
