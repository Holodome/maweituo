package maweituo.postgres.ads.repos

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.{AdId, Advertisement}
import maweituo.domain.users.*
import maweituo.postgres.sql.codecs.given

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import doobie.postgres.implicits.given
import java.time.Instant
import maweituo.domain.Pagination
import maweituo.postgres.utils.*
import maweituo.domain.ads.AdSortOrder
import maweituo.domain.ads.PaginatedAdsResponse
import cats.NonEmptyParallel
import maweituo.domain.PaginatedCollection

object PostgresAdRepo:
  def make[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): AdRepo[F] = new:
    private def adCount: F[Int] =
      sql"select count(*) from advertisements".query[Int].unique.transact(xa)

    def all(pag: Pagination, order: AdSortOrder): F[PaginatedAdsResponse] =
      val base  = fr"select id from advertisements"
      val sort  = adSortOrderToSql(order)
      val limit = fr"limit ${pag.limit} offset ${pag.offset}"
      val query = (base ++ sort ++ limit).query[AdId].to[List].transact(xa)
      (query, adCount).parMapN { (ads, count) =>
        PaginatedAdsResponse(PaginatedCollection(ads, pag, count), order)
      }

    def all: F[List[Advertisement]] =
      sql"select id, author_id, title, is_resolved, created_at, updated_at from advertisements"
        .query[Advertisement]
        .to[List]
        .transact(xa)

    def delete(id: AdId): F[Unit] =
      sql"delete from advertisements where id = $id::uuid".update.run.transact(xa).void

    def create(ad: Advertisement): F[Unit] =
      sql"""
        insert into advertisements(id, author_id, title, is_resolved, created_at, updated_at) 
        values (${ad.id}::uuid, ${ad.authorId}::uuid, ${ad.title}, 
                ${ad.resolved}, ${ad.createdAt}, ${ad.updatedAt})
      """.update.run.transact(xa).void

    def find(id: AdId): OptionT[F, Advertisement] =
      OptionT(
        sql"""select id, author_id, title, is_resolved, created_at, updated_at 
              from advertisements where id = $id::uuid
          """.query[Advertisement].option.transact(xa)
      )

    def findIdsByAuthor(userId: UserId): F[List[AdId]] =
      sql"select id from advertisements where author_id = $userId::uuid"
        .query[AdId]
        .to[List]
        .transact(xa)

    def markAsResolved(id: AdId, at: Instant): F[Unit] =
      sql"""update advertisements 
            set is_resolved = true, updated_at = $at 
            where id = $id::uuid""".update.run.transact(xa).void
