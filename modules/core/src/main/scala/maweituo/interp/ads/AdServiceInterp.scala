package maweituo.interp.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.ads.*
import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.services.AdService
import maweituo.domain.repos.FeedRepo
import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.domain.users.UserId
import maweituo.domain.{Id, Identity}
import maweituo.effects.{GenUUID, TimeSource}

import org.typelevel.log4cats.Logger

object AdServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      ads: AdRepo[F],
      feed: FeedRepo[F]
  )(using iam: IAMService[F], telemetry: TelemetryService[F]): AdService[F] = new:
    def get(id: AdId): F[Advertisement] =
      ads.get(id)

    def create(create: CreateAdRequest)(using authorId: Identity): F[AdId] =
      for
        id <- Id.make[F, AdId]
        ad = Advertisement(id, authorId, create.title, resolved = false)
        _  <- ads.create(ad)
        at <- TimeSource[F].instant
        _  <- feed.addToGlobalFeed(id, at)
        _  <- telemetry.userCreated(authorId, id)
        _  <- Logger[F].info(s"Created ad $id by user $authorId")
      yield id

    def delete(id: AdId)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- ads.delete(id)
        _ <- Logger[F].info(s"Deleted ad $id by user ${summon[Identity]}")
      yield ()

    def markAsResolved(id: AdId, withWhom: UserId)(using Identity): F[Unit] =
      for
        _ <- iam.authAdModification(id)
        _ <- ads.markAsResolved(id)
        _ <- telemetry.userBought(withWhom, id)
      yield ()
