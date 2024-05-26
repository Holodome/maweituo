package com.holodome.cassandra.repositories

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.ads._
import com.holodome.domain.images.ImageId
import com.holodome.domain.repositories.AdvertisementRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.holodome.domain.messages.ChatId

object CassandraAdvertisementRepository {
  def make[F[_]: Async](session: CassandraSession[F]): AdvertisementRepository[F] =
    new CassandraAdvertisementRepository(session)
}

private final class CassandraAdvertisementRepository[F[_]: Async](
    session: CassandraSession[F]
) extends AdvertisementRepository[F] {

  override def addChat(id: AdId, chatId: ChatId): F[Unit] =
    cql"update advertisements set chats = chats - {${chatId.value}} where id = ${id.value}"
      .execute(session)
      .void

  override def create(ad: Advertisement): F[Unit] =
    cql"""insert into advertisements (id, author_id, title, tags, images, chats, resolved)
         |values (${ad.id}, ${ad.authorId}, ${ad.title.str}, ${ad.tags.map(_.str)},
         |${ad.images}, ${ad.chats}, ${ad.resolved})""".stripMargin
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
      .execute(session)
      .void

  override def all: F[List[Advertisement]] =
    cql"select id, author_id, title, tags, images, chats, resolved from advertisements"
      .as[Advertisement]
      .select(session)
      .compile
      .toList

  override def find(id: AdId): OptionT[F, Advertisement] =
    OptionT(findQuery(id).select(session).head.compile.last)

  override def delete(id: AdId): F[Unit] =
    cql"delete from advertisements where id = ${id.value}"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.QUORUM)
      )
      .execute(session)
      .void

  override def addTag(id: AdId, tag: AdTag): F[Unit] =
    cql"update advertisements set tags = tags + {${tag.str}} where id = ${id.value}"
      .execute(session)
      .void

  override def addImage(id: AdId, image: ImageId): F[Unit] =
    cql"update advertisements set images = images + {${image.id}} where id = ${id.value}"
      .execute(session)
      .void

  override def removeTag(id: AdId, tag: AdTag): F[Unit] =
    cql"update advertisements set tags = tags - {${tag.str}} where id = ${id.value}"
      .execute(session)
      .void

  override def removeImage(id: AdId, image: ImageId): F[Unit] =
    cql"update advertisements set images = images - {${image.id}} where id = ${id.value}"
      .execute(session)
      .void

  override def markAsResolved(id: AdId): F[Unit] =
    cql"update advertisements set resolved = true where id = ${id.value}".execute(session).void

  private def findQuery(id: AdId) =
    cql"select id, author_id, title, tags, images, chats, resolved from advertisements where id = ${id.value}"
      .as[Advertisement]

}
