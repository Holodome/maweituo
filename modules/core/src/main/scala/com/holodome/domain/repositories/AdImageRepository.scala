package com.holodome.domain.repositories

import cats.MonadThrow
import cats.data.OptionT
import com.holodome.domain.errors.InvalidImageId
import com.holodome.domain.images._

trait AdImageRepository[F[_]] {
  def create(image: Image): F[Unit]
  def findMeta(imageId: ImageId): OptionT[F, Image]
  def delete(imageId: ImageId): F[Unit]
}

object AdImageRepository {
  implicit class ImageRepositoryOps[F[_]: MonadThrow](repo: AdImageRepository[F]) {
    def getMeta(imageId: ImageId): F[Image] =
      repo.findMeta(imageId).getOrRaise(InvalidImageId(imageId))
  }
}
