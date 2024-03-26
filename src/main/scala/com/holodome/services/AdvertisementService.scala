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
  def find(id: AdvertisementId): F[Advertisement]
  def all(): F[List[Advertisement]]
  def create(authorId: UserId, create: CreateAdRequest): F[Unit]
  def delete(id: AdvertisementId, userId: UserId): F[Unit]

  def authorizeModification(id: AdvertisementId, userId: UserId): F[Unit]
  def addImage(id: AdvertisementId, imageId: ImageId, userId: UserId): F[Unit]
}

object AdvertisementService {
  def make[F[_]: MonadThrow: GenUUID](repo: AdvertisementRepository[F]): AdvertisementService[F] =
    new AdvertisementServiceInterpreter(repo)

  private final class AdvertisementServiceInterpreter[F[_]: MonadThrow: GenUUID](
      repo: AdvertisementRepository[F]
  ) extends AdvertisementService[F] {
    override def find(id: AdvertisementId): F[Advertisement] =
      repo.find(id).getOrElseF(InvalidAdId(id).raiseError)

    override def all(): F[List[Advertisement]] = repo.all()

    override def create(authorId: UserId, create: CreateAdRequest): F[Unit] =
      for {
        id <- Id.make[F, AdvertisementId]
        ad = Advertisement(id, create.title, List(), List(), List(), authorId)
        _ <- repo.create(ad)
      } yield ()

    override def delete(id: AdvertisementId, userId: UserId): F[Unit] =
      authorizeModification(id, userId) *> repo.delete(id)

    override def authorizeModification(id: AdvertisementId, userId: UserId): F[Unit] =
      repo.find(id).getOrElseF(InvalidAdId(id).raiseError[F, Advertisement]).flatMap {
        case ad if ad.authorId === userId => Monad[F].unit
        case _                            => NotAnAuthor().raiseError[F, Unit]
      }

    override def addImage(id: AdvertisementId, imageId: ImageId, userId: UserId): F[Unit] =
      authorizeModification(id, userId) *> repo.addImage(id, imageId)
  }
}
