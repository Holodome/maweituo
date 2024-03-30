package com.holodome.repositories.cassandra

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Async
import com.holodome.domain.images._
import com.holodome.repositories.ImageRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.holodome.cql.codecs._

object CassandraImageRepository {
  def make[F[_]: Async](session: CassandraSession[F]): ImageRepository[F] =
    new CassandraImageRepository[F](session)
}

sealed class CassandraImageRepository[F[_]: Async] private (session: CassandraSession[F])
    extends ImageRepository[F] {

  override def create(image: Image): F[Unit] =
    createQuery(image).execute(session).void

  override def getMeta(imageId: ImageId): OptionT[F, Image] =
    OptionT(getMetaQuery(imageId).select(session).head.compile.last)

  override def delete(imageId: ImageId): F[Unit] =
    deleteQuery(imageId).execute(session).void

  private def createQuery(image: Image) =
    cql"insert into local.images (id, ad_id, url) values (${image.id.id}, ${image.adId.value}, ${image.url.value})"

  private def getMetaQuery(imageId: ImageId) =
    cql"select id, ad_id, url from local.images where id = ${imageId.id}".as[Image]

  private def deleteQuery(imageId: ImageId) =
    cql"delete from local.images where id = ${imageId.id}"
}
