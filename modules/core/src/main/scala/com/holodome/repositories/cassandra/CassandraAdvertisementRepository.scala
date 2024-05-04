package com.holodome.repositories.cassandra

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cql.codecs._
import com.holodome.domain.ads._
import com.holodome.domain.images.ImageId
import com.holodome.repositories.AdvertisementRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraAdvertisementRepository {
  def make[F[_]: Async](session: CassandraSession[F]): AdvertisementRepository[F] =
    new CassandraAdvertisementRepository(session)
}

private final class CassandraAdvertisementRepository[F[_]: Async](
    session: CassandraSession[F]
) extends AdvertisementRepository[F] {

  override def create(ad: Advertisement): F[Unit] =
    createQuery(ad).execute(session).void

  override def all: F[List[Advertisement]] =
    allQuery.select(session).compile.toList

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(findQuery(id).select(session).head.compile.last)

  override def delete(id: AdId): F[Unit] = deleteQuery(id).execute(session).void

  override def addTag(id: AdId, tag: AdTag): F[Unit] = addTagQuery(id, tag).execute(session).void

  override def addImage(id: AdId, image: ImageId): F[Unit] =
    addImageQuery(id, image).execute(session).void

  override def removeTag(id: AdId, tag: AdTag): F[Unit] =
    removeTagQuery(id, tag).execute(session).void

  def removeImage(id: AdId, image: ImageId): F[Unit] =
    removeImageQuery(id, image).execute(session).void

  override def markAsResolved(id: AdId): F[Unit] =
    markAsResolvedQ(id).execute(session).void

  private def createQuery(ad: Advertisement) = {
    cql"""insert into advertisements (id, author_id, title, tags, images, chats, resolved)
         |values (${ad.id.value}, ${ad.authorId}, ${ad.title.value}, ${ad.tags},
         |${ad.images}, ${ad.chats}, ${ad.resolved})""".stripMargin
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
  }

  private def allQuery =
    cql"select id, author_id, title, tags, images, chats, resolved from advertisements"
      .as[Advertisement]

  private def findQuery(id: AdId) =
    cql"select id, author_id, title, tags, images, chats, resolved from advertisements where id = ${id.value}"
      .as[Advertisement]

  private def deleteQuery(id: AdId) =
    cql"delete from advertisements where id = ${id.value}".config(
      _.setConsistencyLevel(ConsistencyLevel.QUORUM)
    )

  private def addTagQuery(id: AdId, tag: AdTag) =
    cql"update advertisements set tags = tags + {${tag.value}} where id = ${id.value}"

  private def addImageQuery(id: AdId, image: ImageId) =
    cql"update advertisements set images = images + {${image.id}} where id = ${id.value}"

  private def removeTagQuery(id: AdId, tag: AdTag) =
    cql"update advertisements set tags = tags - {${tag.value}} where id = ${id.value}"

  private def removeImageQuery(id: AdId, image: ImageId) =
    cql"update advertisements set images = images - {${image.id}} where id = ${id.value}"

  private def markAsResolvedQ(id: AdId) =
    cql"update advertisements set resolved = true where id = ${id.value}"
}
