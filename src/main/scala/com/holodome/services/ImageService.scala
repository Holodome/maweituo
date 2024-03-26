package com.holodome.services

import cats.syntax.all._
import cats.Monad
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.images._
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.{ObjectStorage, ObjectStorageIdGen}
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.repositories.ImageRepository

trait ImageService[F[_]] {
  def upload(uploader: UserId, adId: AdvertisementId, contents: ImageContents): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContents]
}

object ImageService {
  def make[F[_]: Monad: GenUUID](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F],
      objectStorage: ObjectStorage[F]
  ): ImageService[F] = new ImageServiceInterpreter(imageRepo, adService, objectStorage)

  private final class ImageServiceInterpreter[F[_]: Monad: GenUUID](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F],
      objectStorage: ObjectStorage[F]
  ) extends ImageService[F] {

    override def upload(
        uploader: UserId,
        adId: AdvertisementId,
        contents: ImageContentsx
    ): F[ImageId] =
      for {
        _       <- adService.authorizeModification(adId, uploader)
        id      <- ObjectStorageIdGen.make
        _       <- objectStorage.put(id, contents.value)
        imageId <- imageRepo.create(adId, ObjectId.toImageUrl(id))
      } yield imageId

    override def delete(imageId: ImageId, authenticated: UserId): F[Unit] =
      authorizeModificationByImageId(imageId, authenticated).flatMap { image =>
        objectStorage.delete(ObjectId.fromImageUrl(image.url))
      } *> imageRepo.delete(imageId)

    override def get(imageId: ImageId): F[ImageContents] =
      imageRepo.getMeta(imageId).flatMap { image =>
        objectStorage.get(ObjectId.fromImageUrl(image.url)).map(ImageContents.apply)
      }

    private def authorizeModificationByImageId(imageId: ImageId, userId: UserId): F[Image] =
      imageRepo
        .getMeta(imageId)
        .flatTap(image => adService.authorizeModification(image.adId, userId))
  }
}
