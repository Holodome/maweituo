package com.holodome.interpreters

import com.holodome.domain.Id
import com.holodome.domain.ads.*
import com.holodome.domain.repositories.{ AdRepository, FeedRepository, TagRepository }
import com.holodome.domain.services.{ AdService, IAMService, TelemetryService }
import com.holodome.domain.users.UserId
import com.holodome.effects.{ GenUUID, TimeSource }

import cats.MonadThrow
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object AdServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      ads: AdRepository[F],
      tags: TagRepository[F],
      feed: FeedRepository[F],
      iam: IAMService[F],
      telemetry: TelemetryService[F]
  ): AdService[F] = new:
    def get(id: AdId): F[Advertisement] =
      ads.get(id)

    def create(authorId: UserId, create: CreateAdRequest): F[AdId] =
      for
        id <- Id.make[F, AdId]
        ad = Advertisement(id, authorId, create.title, resolved = false)
        _  <- ads.create(ad)
        at <- TimeSource[F].instant
        _  <- feed.addToGlobalFeed(id, at)
        _  <- telemetry.userCreated(authorId, id)
        _  <- Logger[F].info(s"Created ad $id by user $authorId")
      yield id

    def delete(id: AdId, userId: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- ads.delete(id)
        _ <- Logger[F].info(s"Deleted ad $id by user $userId")
      yield ()

    def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- tags.addTagToAd(id, tag)
        _ <- Logger[F].info(s"Added tag $tag to ad $id by user $userId")
      yield ()

    def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- Logger[F].info(s"Removed tag $tag from ad $id by user $userId")
      yield ()

    def markAsResolved(id: AdId, userId: UserId, withWhom: UserId): F[Unit] =
      for
        _ <- iam.authAdModification(id, userId)
        _ <- ads.markAsResolved(id)
        _ <- telemetry.userBought(withWhom, id)
      yield ()
