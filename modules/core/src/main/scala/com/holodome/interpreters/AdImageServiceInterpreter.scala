package com.holodome.interpreters

import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.InternalImageUnsync
import com.holodome.domain.images.*
import com.holodome.domain.repositories.*
import com.holodome.domain.services.{ AdImageService, IAMService }
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.{ GenObjectStorageId, ObjectStorage }

import cats.MonadThrow
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object AdImageServiceInterpreter:
  def make[F[_]: MonadThrow: GenObjectStorageId: GenUUID: Logger](
      imageRepo: AdImageRepository[F],
      adRepo: AdvertisementRepository[F],
      objectStorage: ObjectStorage[F],
      iam: IAMService[F]
  ): AdImageService[F] = new:
    def upload(
        uploader: UserId,
        adId: AdId,
        contents: ImageContentsStream[F]
    ): F[ImageId] =
      for
        objectId <- GenObjectStorageId[F].make
        _        <- objectStorage.putStream(objectId, contents.data, contents.dataSize)
        imageId  <- Id.make[F, ImageId]
        image = Image(
          imageId,
          adId,
          objectId,
          contents.contentType,
          contents.dataSize
        )
        _ <- imageRepo.create(image)
        _ <- adRepo.addImage(adId, imageId)
        _ <- Logger[F].info(s"Uploaded image to $adId by $uploader")
      yield imageId

    def delete(imageId: ImageId, authenticated: UserId): F[Unit] =
      for
        _     <- iam.authorizeImageDelete(imageId, authenticated)
        image <- imageRepo.getMeta(imageId)
        _     <- objectStorage.delete(image.url.toObsID)
        _     <- adRepo.removeImage(image.adId, imageId)
        _ <- Logger[F].info(
          s"Deleted image ${image.id} from ad ${image.adId} by user $authenticated"
        )
      yield ()

    def get(imageId: ImageId): F[ImageContentsStream[F]] =
      for
        image <- imageRepo.getMeta(imageId)
        stream <- objectStorage
          .get(image.url.toObsID)
          .map(ImageContentsStream(_, image.mediaType, image.size))
          .getOrRaise(InternalImageUnsync("Image object not found"))
      yield stream
