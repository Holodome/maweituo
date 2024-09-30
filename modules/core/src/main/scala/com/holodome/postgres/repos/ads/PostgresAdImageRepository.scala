package com.holodome.postgres.ads.repos

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.repos.AdImageRepository
import com.holodome.domain.images.{ Image, ImageId }
import com.holodome.postgres.sql.codecs.given

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.given

object PostgresAdImageRepository:
  def make[F[_]: Async](xa: Transactor[F]): AdImageRepository[F] = new:

    def create(image: Image): F[Unit] =
      sql"""
        insert into images(id, ad_id, url, media_type, size)
        values (${image.id}, ${image.adId}, ${image.url}, ${image.mediaType}, ${image.size})
      """.update.run.transact(xa).void

    def find(imageId: ImageId): OptionT[F, Image] =
      OptionT(
        sql"select id, ad_id, url, media_type, size from images where id = $imageId".query[Image].option.transact(xa)
      )

    def findIdsByAd(adId: AdId): F[List[ImageId]] =
      sql"select id from images where ad_id = $adId".query[ImageId].to[List].transact(xa)

    def delete(imageId: ImageId): F[Unit] =
      sql"delete from images where id = $imageId".update.run.transact(xa).void
