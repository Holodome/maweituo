package com.holodome.services

import cats.syntax.all._
import cats.Monad
import com.holodome.domain.advertisements.AdId
import com.holodome.domain.images._
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.{ObjectStorage, ObjectStorageIdGen}
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.repositories.ImageRepository

trait ImageService[F[_]] {
  def upload(uploader: UserId, adId: AdId, contents: ImageContents): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContents]
}

object ImageService {
  def make[F[_]: Monad: GenUUID](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F],
      objectStorage: ObjectStorage[F],
      iam: IAMService[F]
  ): ImageService[F] = new ImageServiceInterpreter(imageRepo, adService, objectStorage, iam)

  private final class ImageServiceInterpreter[F[_]: Monad: GenUUID](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F],
      objectStorage: ObjectStorage[F],
      iam: IAMService[F]
  ) extends ImageService[F] {

    override def upload(
        uploader: UserId,
        adId: AdId,
        contents: ImageContents
    ): F[ImageId] =
      for {
        id      <- ObjectStorageIdGen.make
        _       <- objectStorage.put(id, contents.value)
        imageId <- imageRepo.create(adId, ObjectId.toImageUrl(id))
        _       <- adService.addImage(adId, imageId, uploader)
      } yield imageId

    override def delete(imageId: ImageId, authenticated: UserId): F[Unit] =
      iam.authorizeImageDelete(imageId, authenticated) >> imageRepo
        .getMeta(imageId)
        .flatMap { image =>
          objectStorage.delete(ObjectId.fromImageUrl(image.url))
        } *> imageRepo.delete(imageId)

    override def get(imageId: ImageId): F[ImageContents] =
      imageRepo.getMeta(imageId).flatMap { image =>
        objectStorage.get(ObjectId.fromImageUrl(image.url)).map(ImageContents.apply)
      }

  }
}
