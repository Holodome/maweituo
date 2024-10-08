package maweituo.postgres.repos.ads

import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.ads.AdId
import maweituo.domain.ads.repos.AdSearchRepo
import maweituo.postgres.sql.codecs.given

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import maweituo.domain.Pagination
import maweituo.domain.ads.AdSortOrder
import cats.NonEmptyParallel
import maweituo.domain.PaginatedCollection
import maweituo.domain.ads.AdTag
import cats.data.NonEmptyList

object PostgresAdSearchRepo:
  def make[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): AdSearchRepo[F] = new:

    private def adSortOrderToSql(order: AdSortOrder): Fragment =
      order match
        case AdSortOrder.CreatedAtAsc => fr"order by created_at asc"
        case AdSortOrder.UpdatedAtAsc => fr"order by updated_at desc"
        case AdSortOrder.Alphabetic   => fr"order by title asc"
        case AdSortOrder.Author       => fr"order by (select name from users where id = author_id)"

    private def adCount: F[Int] =
      sql"select count(*) from advertisements".query[Int].unique.transact(xa)

    private def filteredAdCount(allowedTags: NonEmptyList[AdTag]): F[Int] =
      val sql =
        fr"select count(*) from" ++ Fragments.parentheses(
          fr"""
            select distinct a.id 
            from advertisements a
            join tag_ads ta on ta.ad_id = a.id 
            where """ ++ Fragments.in(fr"tag", allowedTags)
        )
      sql.query[Int].unique.transact(xa)

    def all(pag: Pagination, order: AdSortOrder): F[PaginatedCollection[AdId]] =
      val base  = fr"select id from advertisements"
      val sort  = adSortOrderToSql(order)
      val limit = fr"limit ${pag.limit} offset ${pag.offset}"
      val query = (base ++ sort ++ limit).query[AdId].to[List].transact(xa)
      (query, adCount).parMapN { (ads, count) =>
        PaginatedCollection(ads, pag, count)
      }

    def allFiltered(
        pag: Pagination,
        order: AdSortOrder,
        allowedTags: NonEmptyList[AdTag]
    ): F[PaginatedCollection[AdId]] =
      val base =
        fr"""
      select distinct a.id from advertisements a
      join tag_ads ta on ta.ad_id = a.id 
      where """ ++ Fragments.in(fr"tag", allowedTags)
      val sort  = adSortOrderToSql(order)
      val limit = fr"limit ${pag.limit} offset ${pag.offset}"
      val query = (base ++ sort ++ limit).query[AdId].to[List].transact(xa)
      (query, filteredAdCount(allowedTags)).parMapN { (ads, count) =>
        PaginatedCollection(ads, pag, count)
      }
