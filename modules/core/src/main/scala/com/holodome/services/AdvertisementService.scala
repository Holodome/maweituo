package com.holodome.services

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.domain.errors.InvalidAdId
import com.holodome.domain.images.ImageId
import com.holodome.effects.GenUUID
import com.holodome.repositories.{AdvertisementRepository, TagRepository}

trait AdvertisementService[F[_]] {
  def get(id: AdId): F[Advertisement]
  def all: F[List[Advertisement]]
  def create(authorId: UserId, create: CreateAdRequest): F[AdId]
  def delete(id: AdId, userId: UserId): F[Unit]
  def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
  def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
}

object AdvertisementService {
  def make[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F],
      tags: TagRepository[F],
      iam: IAMService[F]
  ): AdvertisementService[F] =
    new AdvertisementServiceInterpreter(repo, tags, iam)

  private final class AdvertisementServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F],
      tags: TagRepository[F],
      iam: IAMService[F]
  ) extends AdvertisementService[F] {
    override def get(id: AdId): F[Advertisement] =
      repo.get(id)

    override def all: F[List[Advertisement]] = repo.all

    override def create(authorId: UserId, create: CreateAdRequest): F[AdId] =
      for {
        id <- Id.make[F, AdId]
        ad = Advertisement(id, create.title, Set(), Set(), Set(), authorId)
        _ <- repo.create(ad)
      } yield id

    override def delete(id: AdId, userId: UserId): F[Unit] =
      iam.authorizeAdModification(id, userId) *> repo.delete(id)

    override def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] =
      for {
        _ <- iam.authorizeAdModification(id, userId)
        _ <- repo.addTag(id, tag)
        _ <- tags.addTagToAd(id, tag)
      } yield ()

    override def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit] = {
      for {
        _ <- iam.authorizeAdModification(id, userId)
        _ <- tags.removeTagFromAd(id, tag)
        _ <- repo.removeTag(id, tag)
      } yield ()
    }
  }
}
