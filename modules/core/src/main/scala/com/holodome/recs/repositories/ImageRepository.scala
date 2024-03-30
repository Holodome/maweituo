package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.images._

trait ImageRepository[F[_]] {
  def create(image: Image): F[Unit]
  def getMeta(imageId: ImageId): OptionT[F, Image]
  def delete(imageId: ImageId): F[Unit]
}
