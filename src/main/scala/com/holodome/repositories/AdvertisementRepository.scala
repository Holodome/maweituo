package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements._
import com.holodome.domain.users.UserId

trait AdvertisementRepository[F[_]] {
  def create(ad: Advertisement): F[Unit]
  def all(): F[List[Advertisement]]
  def find(id: AdvertisementId): OptionT[F, Advertisement]
  def delete(id: AdvertisementId): F[Unit]
  def addTag(id: AdvertisementId, tag: AdvertisementTag): F[Unit]
  def removeTag(id: AdvertisementId, tag: AdvertisementTag): F[Unit]

  def findByUser(uid: UserId): F[List[Advertisement]]
  def findByTag(tag: AdvertisementTag): F[List[Advertisement]]
}
