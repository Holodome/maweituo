package maweituo
package logic
package interp
package ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*
import maweituo.infrastructure.effects.{GenUUID, TimeSource}
import maweituo.utils.Id

import org.typelevel.log4cats.Logger

object AdServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      ads: AdRepo[F]
  )(using iam: IAMService[F], telemetry: TelemetryService[F]): AdService[F] = new:
    def get(id: AdId): F[Advertisement] =
      ads.get(id)

    def create(create: CreateAdRequest)(using authorId: Identity): F[AdId] =
      for
        id <- Id.make[F, AdId]
        at <- TimeSource[F].instant
        ad = Advertisement(id, authorId, create.title, resolved = false, createdAt = at, updatedAt = at)
        _ <- ads.create(ad)
        _ <- telemetry.userCreated(authorId, id)
        _ <- Logger[F].info(s"Created ad $id by user $authorId")
      yield id

    def delete(id: AdId)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- ads.delete(id)
        _ <- Logger[F].info(s"Deleted ad $id by user ${summon[Identity]}")
      yield ()

    def markAsResolved(id: AdId, withWhom: UserId)(using Identity): F[Unit] =
      for
        at <- TimeSource[F].instant
        _  <- iam.authAdModification(id)
        _  <- ads.markAsResolved(id, at)
        _  <- telemetry.userBought(withWhom, id)
      yield ()
