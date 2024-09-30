package maweituo.postgres.ads.repos

import maweituo.domain.ads.repos.AdRepository
import maweituo.domain.ads.{AdId, Advertisement}
import maweituo.domain.users.*
import maweituo.postgres.sql.codecs.given

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*

object PostgresAdRepository:
  def make[F[_]: Async](xa: Transactor[F]): AdRepository[F] = new:
    def all: F[List[Advertisement]] =
      sql"select id, author_id, title, is_resolved from advertisements"
        .query[Advertisement]
        .to[List]
        .transact(xa)

    def delete(id: AdId): F[Unit] =
      sql"delete from advertisements where id = $id".update.run.transact(xa).void

    def create(ad: Advertisement): F[Unit] =
      sql"""
        insert into advertisements(id, author_id, title, is_resolved) " +
        values (${ad.id}, ${ad.authorId}, ${ad.title}, ${ad.resolved})
      """.update.run.transact(xa).void

    def find(id: AdId): OptionT[F, Advertisement] =
      OptionT(
        sql"select id, author_id, title, is_resolved from advertisements where id = $id"
          .query[Advertisement].option.transact(xa)
      )

    def findIdsByAuthor(userId: UserId): F[List[AdId]] =
      sql"select id from advertisements where author_id = $userId"
        .query[AdId]
        .to[List]
        .transact(xa)

    def markAsResolved(id: AdId): F[Unit] =
      sql"update advertisements where id = $id set is_resolved = true".update.run.transact(xa).void
