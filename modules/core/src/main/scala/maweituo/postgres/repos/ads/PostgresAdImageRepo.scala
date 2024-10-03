package maweituo.postgres.ads.repos

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.given

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.{Image, ImageId, ImageUrl, MediaType}
import maweituo.domain.ads.repos.AdImageRepo
import maweituo.postgres.sql.codecs.given

import doobie.Transactor
import doobie.implicits.given

object PostgresAdImageRepo:
  def make[F[_]: Async](xa: Transactor[F]): AdImageRepo[F] = new:

    def create(image: Image): F[Unit] =
      sql"""
        insert into images(id, ad_id, url, media_type, size)
        values (${image.id}::uuid, ${image.adId}::uuid, ${image.url}, ${image.mediaType.toRaw}, ${image.size})
      """.update.run.transact(xa).void

    def find(imageId: ImageId): OptionT[F, Image] =
      OptionT(
        sql"select id, ad_id, url, media_type, size from images where id = $imageId::uuid"
          .query[(ImageId, AdId, ImageUrl, String, Long)]
          .option
          .transact(xa)
      ).flatMap { (ad, adId, url, media, size) =>
        OptionT.fromOption(MediaType.fromRaw(media)).orElseF(
          Async[F].raiseError(new Exception("invalid media type"))
        ).map(media =>
          Image(ad, adId, url, media, size)
        )
      }

    def findIdsByAd(adId: AdId): F[List[ImageId]] =
      sql"select id from images where ad_id = $adId::uuid".query[ImageId].to[List].transact(xa)

    def delete(imageId: ImageId): F[Unit] =
      sql"delete from images where id = $imageId::uuid".update.run.transact(xa).void
