package maweituo
package postgres
package repos
package ads

import cats.data.OptionT
import cats.effect.MonadCancelThrow
import cats.syntax.all.*

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import doobie.postgres.implicits.given
import java.time.Instant
import cats.Applicative
import cats.data.NonEmptyList

object PostgresAdRepo:
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): AdRepo[F] = new:

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

    def update(update: UpdateAdRepoRequest): F[Unit] =
      if update.resolved.isEmpty && update.title.isEmpty then
        Applicative[F].unit
      else
        updateQuery(update).update.run.transact(xa).void

    private def updateQuery(update: UpdateAdRepoRequest) =
      val sets = NonEmptyList(
        fr"updated_at = ${update.at}",
        List(
          update.resolved.map(resolved => fr" is_resolved = $resolved "),
          update.title.map(_.value).map(title => fr" title = $title ")
        ).flatten
      )
      val id = update.id
      (fr"update users set " ++ sets.reduce(_ ++ fr"," ++ _) ++ fr" where id = $id::uuid")
