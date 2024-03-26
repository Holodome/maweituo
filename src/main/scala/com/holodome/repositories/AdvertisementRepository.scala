package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements._
import com.holodome.domain.images.ImageId

trait AdvertisementRepository[F[_]] {
  def create(ad: Advertisement): F[Unit]
  def all(): F[List[Advertisement]]
  def find(id: AdvertisementId): OptionT[F, Advertisement]
  def delete(id: AdvertisementId): F[Unit]
  def addTag(id: AdvertisementId, tag: AdvertisementTag): F[Unit]
  def addImage(id: AdvertisementId, image: ImageId): F[Unit]
  def removeTag(id: AdvertisementId, tag: AdvertisementTag): F[Unit]
}
