package com.holodome.repositories.cassandra

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Async
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.domain.ads._
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.repositories.AdvertisementRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.holodome.ext.cassandra4io.typeMappers._
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.ringcentral.cassandra4io.cql.Reads._
import com.holodome.cql.codecs._

object CassandraAdvertisementRepository {
  def make[F[_]: Async](session: CassandraSession[F]): AdvertisementRepository[F] =
    new CassandraAdvertisementRepository(session)
}

sealed class CassandraAdvertisementRepository[F[_]: Async] private (session: CassandraSession[F])
    extends AdvertisementRepository[F] {

  private case class SerializedAd(
      id: AdId,
      authorId: UserId,
      title: AdTitle,
      tags: Option[Set[AdTag]],
      images: Option[Set[ImageId]],
      chats: Option[Set[ChatId]]
  ) {
    def toDomain: Advertisement =
      Advertisement(
        id,
        title,
        tags.getOrElse(Set()),
        images.getOrElse(Set()),
        chats.getOrElse(Set()),
        authorId
      )
  }

  override def create(ad: Advertisement): F[Unit] =
    createQuery(ad).execute(session).void

  override def all(): F[List[Advertisement]] =
    allQuery.select(session).compile.toList.map(_.map(_.toDomain))

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(findQuery(id).select(session).head.compile.last).map(_.toDomain)

  override def delete(id: AdId): F[Unit] = deleteQuery(id).execute(session).void

  override def addTag(id: AdId, tag: AdTag): F[Unit] = addTagQuery(id, tag).execute(session).void

  override def addImage(id: AdId, image: ImageId): F[Unit] =
    addImageQuery(id, image).execute(session).void

  override def removeTag(id: AdId, tag: AdTag): F[Unit] =
    removeTagQuery(id, tag).execute(session).void

  private def createQuery(ad: Advertisement) = {
    cql"""insert into advertisements (id, author_id, title, tags, images, chats)
         |values (${ad.id.value}, ${ad.authorId}, ${ad.title.value}, ${ad.tags},
         |${ad.images}, ${ad.chats})""".stripMargin
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
  }

  private def allQuery =
    cql"select id, author_id, title, tags, images, chats from advertisements"
      .as[SerializedAd]

  private def findQuery(id: AdId) =
    cql"select id, author_id, title, tags, images, chats from advertisements where id = ${id.value}"
      .as[SerializedAd]

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

}
