package com.holodome.services

import cats.syntax.all._
import cats.{MonadThrow, NonEmptyParallel}
import com.holodome.domain.ads.AdId
import com.holodome.domain.images._
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.{GenObjectStorageId, ObjectStorage}
import com.holodome.infrastructure.ObjectStorage.ObjectId
import com.holodome.repositories.ImageRepository

trait ImageService[F[_]] {
  def upload(uploader: UserId, adId: AdId, contents: ImageContents): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContents]
}

object ImageService {
  def make[F[_]: MonadThrow: GenObjectStorageId: GenUUID: NonEmptyParallel](
      imageRepo: ImageRepository[F],
      adService: AdvertisementService[F],
      objectStorage: ObjectStorage[F],
      iam: IAMService[F]
  ): ImageService[F] = new ImageServiceInterpreter(imageRepo, adService, objectStorage, iam)

  private final class ImageServiceInterpreter[F[
      _
  ]: MonadThrow: GenObjectStorageId: GenUUID: NonEmptyParallel](
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
        objectId <- GenObjectStorageId[F].make
        _        <- objectStorage.put(objectId, contents.value)
        imageId  <- Id.make[F, ImageId]
        image = Image(
          imageId,
          adId,
          ObjectId.toImageUrl(objectId)
        )
        _ <- imageRepo.create(image)
        _ <- adService.addImage(adId, imageId, uploader)
      } yield imageId

    override def delete(imageId: ImageId, authenticated: UserId): F[Unit] =
      iam.authorizeImageDelete(imageId, authenticated) >> {
        find(imageId)
          .flatMap { image =>
            objectStorage.delete(ObjectId.fromImageUrl(image.url))
          } &> imageRepo.delete(imageId)
      }

    override def get(imageId: ImageId): F[ImageContents] =
      find(imageId)
        .flatMap { image =>
          objectStorage
            .get(ObjectId.fromImageUrl(image.url))
            .map(ImageContents.apply)
            .getOrRaise(InternalImageUnsync())
        }

    private def find(id: ImageId): F[Image] =
      imageRepo
        .getMeta(id)
        .getOrRaise(InvalidImageId())
  }
}
