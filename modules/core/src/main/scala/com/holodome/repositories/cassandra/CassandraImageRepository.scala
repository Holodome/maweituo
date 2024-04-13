package com.holodome.repositories.cassandra

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Async
import com.holodome.domain.images._
import com.holodome.repositories.ImageRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.holodome.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.DatabaseEncodingError
import org.http4s.LiteralSyntaxMacros.mediaType

object CassandraImageRepository {
  def make[F[_]: Async](session: CassandraSession[F]): ImageRepository[F] =
    new CassandraImageRepository[F](session)
}

sealed class CassandraImageRepository[F[_]: Async] private (session: CassandraSession[F])
    extends ImageRepository[F] {

  private case class SerializedImage(
      id: ImageId,
      adId: AdId,
      url: ImageUrl,
      mediaType: String,
      size: Long
  ) {
    def toDomain: F[Image] =
      OptionT
        .fromOption(MediaType.fromRaw(mediaType))
        .getOrRaise(DatabaseEncodingError())
        .map(
          Image(id, adId, url, _, size)
        )
  }

  override def create(image: Image): F[Unit] =
    createQuery(image).execute(session).void

  override def getMeta(imageId: ImageId): OptionT[F, Image] =
    OptionT(getMetaQuery(imageId).select(session).head.compile.last)
      .flatMap(i => OptionT.liftF(i.toDomain))

  override def delete(imageId: ImageId): F[Unit] =
    deleteQuery(imageId).execute(session).void

  private def createQuery(image: Image) =
    cql"insert into local.images (id, ad_id, url, media_type, size) values (${image.id.id}, ${image.adId.value}, ${image.url.value}, ${image.mediaType.toRaw}, ${image.size})"

  private def getMetaQuery(imageId: ImageId) =
    cql"select id, ad_id, url, media_type, size from local.images where id = ${imageId.id}"
      .as[SerializedImage]

  private def deleteQuery(imageId: ImageId) =
    cql"delete from local.images where id = ${imageId.id}"
}
