package maweituo.interp.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.*
import maweituo.domain.ads.repos.{AdImageRepo, AdRepo}
import maweituo.domain.errors.DomainError
import maweituo.domain.services.{AdImageService, IAMService}
import maweituo.domain.{Id, Identity}
import maweituo.effects.GenUUID
import maweituo.infrastructure.{GenObjectStorageId, ObjectStorage}

import org.typelevel.log4cats.Logger

object AdImageServiceInterp:
  def make[F[_]: MonadThrow: GenObjectStorageId: GenUUID: Logger](
      imageRepo: AdImageRepo[F],
      adRepo: AdRepo[F],
      objectStorage: ObjectStorage[F]
  )(using iam: IAMService[F]): AdImageService[F] = new:
    def upload(
        adId: AdId,
        contents: ImageContentsStream[F]
    )(using Identity): F[ImageId] =
      for
        _        <- iam.authAdModification(adId)
        objectId <- GenObjectStorageId[F].genId
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
        _ <- Logger[F].info(s"Uploaded image to $adId by ${summon[Identity]}")
      yield imageId

    def delete(imageId: ImageId)(using Identity): F[Unit] =
      for
        _     <- iam.authImageDelete(imageId)
        image <- imageRepo.get(imageId)
        _     <- objectStorage.delete(image.url)
        _     <- imageRepo.delete(imageId)
        _ <- Logger[F].info(
          s"Deleted image ${image.id} from ad ${image.adId} by user ${summon[Identity]}"
        )
      yield ()

    def get(imageId: ImageId): F[ImageContentsStream[F]] =
      for
        image <- imageRepo.get(imageId)
        stream <- objectStorage
          .get(image.url)
          .map(ImageContentsStream(_, image.mediaType, image.size))
          .getOrRaise(DomainError.InternalImageUnsync("Image object not found"))
      yield stream
