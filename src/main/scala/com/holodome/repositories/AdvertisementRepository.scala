package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements._
import com.holodome.domain.images.ImageId

trait AdvertisementRepository[F[_]] {
  def create(ad: Advertisement): F[Unit]
  def all(): F[List[Advertisement]]
  def find(id: AdId): OptionT[F, Advertisement]
  def delete(id: AdId): F[Unit]
  def addTag(id: AdId, tag: AdTag): F[Unit]
  def addImage(id: AdId, image: ImageId): F[Unit]
  def removeTag(id: AdId, tag: AdTag): F[Unit]
}
