package com.holodome.services.interpreters

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.InternalImageUnsync
import com.holodome.domain.images.Image
import com.holodome.domain.images.ImageContentsStream
import com.holodome.domain.images._
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.GenObjectStorageId
import com.holodome.infrastructure.ObjectStorage
import com.holodome.repositories.AdImageRepository
import com.holodome.repositories.AdvertisementRepository
import com.holodome.services.AdImageService
import com.holodome.services.IAMService
import org.typelevel.log4cats.Logger

object AdImageServiceInterpreter {

  def make[F[_]: MonadThrow: GenObjectStorageId: GenUUID: Logger](
      imageRepo: AdImageRepository[F],
      adRepo: AdvertisementRepository[F],
      objectStorage: ObjectStorage[F],
      iam: IAMService[F]
  ): AdImageService[F] = new AdImageServiceInterpreter(imageRepo, adRepo, objectStorage, iam)

}

private final class AdImageServiceInterpreter[
    F[_]: MonadThrow: GenObjectStorageId: GenUUID: Logger
](
    imageRepo: AdImageRepository[F],
    adRepo: AdvertisementRepository[F],
    objectStorage: ObjectStorage[F],
    iam: IAMService[F]
) extends AdImageService[F] {

  override def upload(
      uploader: UserId,
      adId: AdId,
      contents: ImageContentsStream[F]
  ): F[ImageId] = for {
    objectId <- GenObjectStorageId[F].make
    _        <- objectStorage.putStream(objectId, contents.data, contents.dataSize)
    imageId  <- Id.make[F, ImageId]
    image = Image(
      imageId,
      adId,
      objectId.toImageUrl,
      contents.contentType,
      contents.dataSize
    )
    _ <- imageRepo.create(image)
    _ <- adRepo.addImage(adId, imageId)
    _ <- Logger[F].info(s"Uploaded image to $adId by $uploader")
  } yield imageId

  override def delete(imageId: ImageId, authenticated: UserId): F[Unit] = for {
    _     <- iam.authorizeImageDelete(imageId, authenticated)
    image <- imageRepo.getMeta(imageId)
    _     <- objectStorage.delete(image.url.toObsID)
    _     <- adRepo.removeImage(image.adId, imageId)
    _ <- Logger[F].info(
      s"Deleted image ${image.id} from ad ${image.adId} by user $authenticated"
    )
  } yield ()

  override def get(imageId: ImageId): F[ImageContentsStream[F]] = for {
    image <- imageRepo.getMeta(imageId)
    stream <- objectStorage
      .get(image.url.toObsID)
      .map(ImageContentsStream(_, image.mediaType, image.size))
      .getOrRaise(InternalImageUnsync("Image object not found"))
  } yield stream

}
