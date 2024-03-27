package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.domain.images.ImageId
import com.holodome.effects.GenUUID
import com.holodome.repositories.AdvertisementRepository

trait AdvertisementService[F[_]] {
  def find(id: AdId): F[Advertisement]
  def all(): F[List[Advertisement]]
  def create(authorId: UserId, create: CreateAdRequest): F[AdId]
  def delete(id: AdId, userId: UserId): F[Unit]
  def addImage(id: AdId, imageId: ImageId, userId: UserId): F[Unit]
}

object AdvertisementService {
  def make[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F],
      iam: IAMService[F]
  ): AdvertisementService[F] =
    new AdvertisementServiceInterpreter(repo, iam)

  private final class AdvertisementServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F],
      iam: IAMService[F]
  ) extends AdvertisementService[F] {
    override def find(id: AdId): F[Advertisement] =
      repo.find(id).getOrElseF(InvalidAdId(id).raiseError)

    override def all(): F[List[Advertisement]] = repo.all()

    override def create(authorId: UserId, create: CreateAdRequest): F[AdId] =
      for {
        id <- Id.make[F, AdId]
        ad = Advertisement(id, create.title, List(), List(), List(), authorId)
        _ <- repo.create(ad)
      } yield id

    override def delete(id: AdId, userId: UserId): F[Unit] =
      iam.authorizeAdModification(id, userId) >> repo.delete(id)

    override def addImage(id: AdId, imageId: ImageId, userId: UserId): F[Unit] =
      iam.authorizeAdModification(id, userId) >> repo.addImage(id, imageId)
  }
}
