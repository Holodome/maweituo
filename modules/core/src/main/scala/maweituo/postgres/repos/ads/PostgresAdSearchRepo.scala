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
import maweituo.domain.users.UserId
import maweituo.domain.ads.AdSearchRequest

object PostgresAdSearchRepo:
  def make[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): AdSearchRepo[F] = new:

    private def adSortOrderToSql(order: AdSortOrder): Fragment =
      order match
        case AdSortOrder.CreatedAtAsc => fr"order by created_at asc"
        case AdSortOrder.UpdatedAtAsc => fr"order by updated_at desc"
        case AdSortOrder.Alphabetic   => fr"order by title asc"
        case AdSortOrder.Author       => fr"order by (select name from users where id = author_id) asc"
        case AdSortOrder.Recs(user) => fr"""
          order by (select aw.embedding <=> (select embedding from user_weights where us = $user::uuid)
                    from ad_weights aw where aw.ad_id = id)
        """

    private def doFind(base: Fragment, pag: Pagination, order: AdSortOrder): F[PaginatedCollection[AdId]] =
      val filteredAdCount = (fr"select count(*) from" ++ Fragments.parentheses(base) ++ fr"as t")
        .query[Int].unique.transact(xa)
      val sort  = adSortOrderToSql(order)
      val limit = fr"limit ${pag.limit} offset ${pag.offset}"
      val query = (base ++ sort ++ limit).query[AdId].to[List].transact(xa)
      (query, filteredAdCount).parMapN { (ads, count) =>
        PaginatedCollection(ads, pag, count)
      }

    private def constructBase(filterTags: Option[NonEmptyList[AdTag]], nameLike: Option[String]): Fragment =
      (filterTags, nameLike) match
        case (Some(t), Some(n)) =>
          fr"""
      select distinct a.id 
      from advertisements a
      join tag_ads ta on ta.ad_id = a.id""" ++ Fragments.whereAnd(
            Fragments.in(fr"tag", t),
            fr"title ilike $n"
          )
        case (Some(t), None) =>
          fr"""
      select distinct a.id 
      from advertisements a
      join tag_ads ta on ta.ad_id = a.id 
      where """ ++ Fragments.in(fr"tag", t)
        case (None, Some(n)) => fr"select id from advertisements where title ilike $n"
        case (None, None)    => fr"select id from advertisements"

    def search(req: AdSearchRequest): F[PaginatedCollection[AdId]] =
      val base = constructBase(req.filterTags, req.nameLike)
      doFind(base, req.pag, req.order)
