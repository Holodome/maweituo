package com.holodome.services

import cats.{Applicative, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.{GenUUID, TimeSource}
import com.holodome.repositories.{AdvertisementRepository, FeedRepository, TagRepository}
import org.typelevel.log4cats.Logger

trait AdvertisementService[F[_]] {
  def get(id: AdId): F[Advertisement]
  def create(authorId: UserId, create: CreateAdRequest): F[AdId]
  def delete(id: AdId, userId: UserId): F[Unit]
  def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
  def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
  def markAsResolved(id: AdId, userId: UserId, withWhom: UserId): F[Unit]
}

object AdvertisementService {
  def make[F[_]: MonadThrow: GenUUID: Logger: TimeSource](
      repo: AdvertisementRepository[F],
      tags: TagRepository[F],
      feed: FeedRepository[F],
      iam: IAMService[F],
      telemetry: TelemetryService[F]
  ): AdvertisementService[F] =
    new AdvertisementServiceInterpreter(repo, tags, feed, iam, telemetry)

  private final class AdvertisementServiceInterpreter[
      F[_]: MonadThrow: GenUUID: Logger: TimeSource
  ](
      repo: AdvertisementRepository[F],
      tags: TagRepository[F],
      feed: FeedRepository[F],
      iam: IAMService[F],
      telemetry: TelemetryService[F]
  ) extends AdvertisementService[F] {
    override def get(id: AdId): F[Advertisement] =
      repo.get(id)

    override def create(authorId: UserId, create: CreateAdRequest): F[AdId] = for {
      id <- Id.make[F, AdId]
      ad = Advertisement(id, authorId, create.title, Set(), Set(), Set(), resolved = false)
      _  <- repo.create(ad)
      at <- TimeSource[F].instant
      _  <- feed.addToGlobalFeed(id, at)
      _  <- telemetry.userCreated(authorId, id)
      _  <- Logger[F].info(s"Created ad $id by user $authorId")
    } yield id

    override def delete(id: AdId, userId: UserId): F[Unit] = for {
      _ <- iam.authorizeAdModification(id, userId)
      _ <- repo.delete(id)
      _ <- Logger[F].info(s"Deleted ad $id by user $userId")
    } yield ()

    override def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] = for {
      _ <- iam.authorizeAdModification(id, userId)
      _ <- tags.ensureCreated(tag).flatMap {
        case true  => Logger[F].info(s"Created new tag $tag")
        case false => Applicative[F].unit
      }
      _ <- repo.addTag(id, tag)
      _ <- tags.addTagToAd(id, tag)
      _ <- Logger[F].info(s"Added tag $tag to ad $id by user $userId")
    } yield ()

    override def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] = for {
      _ <- iam.authorizeAdModification(id, userId)
      _ <- tags.removeTagFromAd(id, tag)
      _ <- repo.removeTag(id, tag)
      _ <- Logger[F].info(s"Removed tag $tag from ad $id by user $userId")
    } yield ()

    override def markAsResolved(id: AdId, userId: UserId, withWhom: UserId): F[Unit] = for {
      _ <- iam.authorizeAdModification(id, userId)
      _ <- repo.markAsResolved(id)
      _ <- telemetry.userBought(withWhom, id)
    } yield ()
  }
}
