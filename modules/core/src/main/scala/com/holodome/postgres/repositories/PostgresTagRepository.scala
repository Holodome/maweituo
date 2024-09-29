package com.holodome.postgres.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag
import com.holodome.domain.repositories.TagRepository
import com.holodome.postgres.sql.codecs.given

import cats.effect.Async
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

object PostgresTagRepository:
  def make[F[_]: Async](xa: Transactor[F]): TagRepository[F] = new:
    def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
      sql"insert into tag_ads(tag, ad_id) values ($adId, $tag)".update.run.transact(xa).void

    def getAllAdsByTag(tag: AdTag): F[List[AdId]] =
      sql"select distinct ad_id from tag_ads where tag = $tag"
        .query[AdId].to[List].transact(xa)

    def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
      sql"delete * from tag_ads where ad_id = $adId and tag = $tag".update.run.transact(xa).void

    def getAllTags: F[List[AdTag]] =
      sql"select distinct tag from tag_ads".query[AdTag].to[List].transact(xa)
