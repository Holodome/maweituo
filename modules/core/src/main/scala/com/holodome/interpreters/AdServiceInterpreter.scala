package com.holodome.interpreters

import com.holodome.domain.Id
import com.holodome.domain.ads.*
import com.holodome.domain.repositories.UserAdsRepository
import com.holodome.domain.repositories.{ AdvertisementRepository, FeedRepository, TagRepository }
import com.holodome.domain.services.{ AdService, IAMService, TelemetryService }
import com.holodome.domain.users.UserId
import com.holodome.effects.{ GenUUID, TimeSource }

import cats.syntax.all.*
import cats.{ Applicative, MonadThrow }
import org.typelevel.log4cats.Logger

object AdServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      repo: AdvertisementRepository[F],
      tags: TagRepository[F],
      feed: FeedRepository[F],
      userAdRepo: UserAdsRepository[F],
      iam: IAMService[F],
      telemetry: TelemetryService[F]
  ): AdService[F] = new:
    def get(id: AdId): F[Advertisement] =
      repo.get(id)

    def create(authorId: UserId, create: CreateAdRequest): F[AdId] =
      for
        id <- Id.make[F, AdId]
        ad = Advertisement(id, authorId, create.title, Set(), Set(), Set(), resolved = false)
        _  <- repo.create(ad)
        _  <- userAdRepo.create(authorId, id)
        at <- TimeSource[F].instant
        _  <- feed.addToGlobalFeed(id, at)
        _  <- telemetry.userCreated(authorId, id)
        _  <- Logger[F].info(s"Created ad $id by user $authorId")
      yield id

    def delete(id: AdId, userId: UserId): F[Unit] =
      for
        _ <- iam.authorizeAdModification(id, userId)
        _ <- repo.delete(id)
        _ <- Logger[F].info(s"Deleted ad $id by user $userId")
      yield ()

    def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authorizeAdModification(id, userId)
        _ <- tags.ensureCreated(tag).flatMap {
          case true  => Applicative[F].unit
          case false => Logger[F].info(s"Created new tag $tag")
        }
        _ <- repo.addTag(id, tag)
        _ <- tags.addTagToAd(id, tag)
        _ <- Logger[F].info(s"Added tag $tag to ad $id by user $userId")
      yield ()

    def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for
        _ <- iam.authorizeAdModification(id, userId)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- repo.removeTag(id, tag)
        _ <- Logger[F].info(s"Removed tag $tag from ad $id by user $userId")
      yield ()

    def markAsResolved(id: AdId, userId: UserId, withWhom: UserId): F[Unit] =
      for
        _ <- iam.authorizeAdModification(id, userId)
        _ <- repo.markAsResolved(id)
        _ <- telemetry.userBought(withWhom, id)
      yield ()
