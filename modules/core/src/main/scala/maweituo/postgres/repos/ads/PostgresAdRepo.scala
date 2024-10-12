package maweituo
package postgres
package repos
package ads

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import doobie.postgres.implicits.given
import java.time.Instant
import cats.NonEmptyParallel

object PostgresAdRepo:
  def make[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): AdRepo[F] = new:

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
