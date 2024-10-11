package maweituo
package postgres
package repos

import maweituo.domain.all.*

import doobie.*
import doobie.implicits.*
export doobie.implicits.given
import doobie.Transactor
import doobie.postgres.implicits.given
import java.time.Instant

object PostgresTelemetryRepo:
  def make[F[_]: Async](xa: Transactor[F]): TelemetryRepo[F] = new:

    def userViewed(user: UserId, ad: AdId, at: Instant): F[Unit] =
      sql"insert into user_viewed(us, ad, at) values ($user::uuid, $ad::uuid, $at)"
        .update.run.transact(xa).void

    def userCreated(user: UserId, ad: AdId, at: Instant): F[Unit] =
      sql"insert into user_created(us, ad, at) values ($user::uuid, $ad::uuid, $at)"
        .update.run.transact(xa).void

    def userBought(user: UserId, ad: AdId, at: Instant): F[Unit] =
      sql"insert into user_bought(us, ad, at) values ($user::uuid, $ad::uuid, $at)"
        .update.run.transact(xa).void

    def userDiscussed(user: UserId, ad: AdId, at: Instant): F[Unit] =
      sql"insert into user_discussed(us, ad, at) values ($user::uuid, $ad::uuid, $at)"
        .update.run.transact(xa).void
