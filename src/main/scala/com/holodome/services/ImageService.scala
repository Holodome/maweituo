package com.holodome.services

import cats.syntax.all._
import cats.Monad
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.images._
import com.holodome.domain.users.UserId
import com.holodome.repositories.ImageRepository

trait ImageService[F[_]] {
  def upload(uploader: UserId, adId: AdvertisementId, contents: ImageContents): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContents]
}

object ImageService {
  def make[F[_]: Monad](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F]
  ): ImageService[F] = new ImageServiceInterpreter(imageRepo, adService)

  private final class ImageServiceInterpreter[F[_]: Monad](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F]
  ) extends ImageService[F] {

    override def upload(
        uploader: UserId,
        adId: AdvertisementId,
        contents: ImageContents
    ): F[ImageId] =
      adService.authorizeModification(adId, uploader) *> imageRepo.create(adId, contents)

    override def delete(imageId: ImageId, authenticated: UserId): F[Unit] =
      authorizeModificationByImageId(imageId, authenticated) *> imageRepo.delete(imageId)

    override def get(imageId: ImageId): F[ImageContents] =
      imageRepo.getContents(imageId)

    private def authorizeModificationByImageId(imageId: ImageId, userId: UserId): F[Unit] =
      imageRepo
        .getMeta(imageId)
        .map(_.adId)
        .flatMap(adService.authorizeModification(_, userId))
  }
}
