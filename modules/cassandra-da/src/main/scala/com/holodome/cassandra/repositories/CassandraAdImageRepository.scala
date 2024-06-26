package com.holodome.cassandra.repositories

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.DatabaseEncodingError
import com.holodome.domain.images._
import com.holodome.domain.repositories.AdImageRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraAdImageRepository {
  def make[F[_]: Async](session: CassandraSession[F]): AdImageRepository[F] =
    new CassandraAdImageRepository[F](session)
}

private final class CassandraAdImageRepository[F[_]: Async](session: CassandraSession[F])
    extends AdImageRepository[F] {

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
        .getOrRaise(DatabaseEncodingError("Failed to deserialize image to domain"))
        .map(
          Image(id, adId, url, _, size)
        )
  }

  override def create(image: Image): F[Unit] =
    cql"insert into local.images (id, ad_id, url, media_type, size) values (${image.id.id}, ${image.adId.value}, ${image.url.value}, ${image.mediaType.toRaw}, ${image.size})"
      .execute(session)
      .void

  override def findMeta(imageId: ImageId): OptionT[F, Image] =
    OptionT(
      cql"select id, ad_id, url, media_type, size from local.images where id = ${imageId.id}"
        .as[SerializedImage]
        .select(session)
        .head
        .compile
        .last
    ).flatMap(i => OptionT.liftF(i.toDomain))

  override def delete(imageId: ImageId): F[Unit] =
    cql"delete from local.images where id = ${imageId.id}".execute(session).void

}
