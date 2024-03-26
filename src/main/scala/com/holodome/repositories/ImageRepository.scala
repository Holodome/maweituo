package com.holodome.repositories

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.images._

trait ImageRepository[F[_]] {
  def create(adId: AdvertisementId, url: ImageUrl): F[ImageId]
  def getMeta(imageId: ImageId): F[Image]
  def delete(imageId: ImageId): F[Unit]
}
