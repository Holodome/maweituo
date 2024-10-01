package maweituo.postgres.ads.repos

import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.ads.repos.AdTagRepo
import maweituo.domain.ads.{AdId, AdTag}
import maweituo.postgres.sql.codecs.given

import doobie.*
import doobie.implicits.*

object PostgresTagRepo:
  def make[F[_]: Async](xa: Transactor[F]): AdTagRepo[F] = new:
    def addTagToAd(adId: AdId, tag: AdTag): F[Unit] =
      sql"insert into tag_ads(tag, ad_id) values ($adId, $tag)".update.run.transact(xa).void

    def getAllAdsByTag(tag: AdTag): F[List[AdId]] =
      sql"select distinct ad_id from tag_ads where tag = $tag"
        .query[AdId].to[List].transact(xa)

    def getAdTags(adId: AdId): F[List[AdTag]] =
      sql"select tag from tag_ads where ad_id = $adId".query[AdTag].to[List].transact(xa)

    def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit] =
      sql"delete * from tag_ads where ad_id = $adId and tag = $tag".update.run.transact(xa).void

    def getAllTags: F[List[AdTag]] =
      sql"select distinct tag from tag_ads".query[AdTag].to[List].transact(xa)
