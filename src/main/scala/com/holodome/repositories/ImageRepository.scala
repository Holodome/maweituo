package com.holodome.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.images._

trait ImageRepository[F[_]] {
  def create(adId: AdId, url: ImageUrl): F[ImageId]
  def getMeta(imageId: ImageId): F[Image]
  def delete(imageId: ImageId): F[Unit]
}
