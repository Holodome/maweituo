package com.holodome.repositories

import cats.data.OptionT
import cats.MonadThrow
import com.holodome.domain.ads._
import com.holodome.domain.errors.InvalidAdId
import com.holodome.domain.images.ImageId

trait AdvertisementRepository[F[_]] {
  def create(ad: Advertisement): F[Unit]
  def all: F[List[Advertisement]]
  def find(id: AdId): OptionT[F, Advertisement]
  def delete(id: AdId): F[Unit]
  def addTag(id: AdId, tag: AdTag): F[Unit]
  def addImage(id: AdId, image: ImageId): F[Unit]
  def removeTag(id: AdId, tag: AdTag): F[Unit]
  def removeImage(id: AdId, image: ImageId): F[Unit]
  def markAsResolved(id: AdId): F[Unit]
}

object AdvertisementRepository {
  implicit class AdvertisementRepositoryOps[F[_]: MonadThrow](repo: AdvertisementRepository[F]) {
    def get(id: AdId): F[Advertisement] =
      repo.find(id).getOrRaise(InvalidAdId(id))
  }
}
