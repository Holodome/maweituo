package com.holodome.domain.repositories

import com.holodome.domain.errors.InvalidImageId
import com.holodome.domain.images.*
import com.holodome.domain.ads.AdId

import cats.MonadThrow
import cats.data.OptionT
import com.holodome.domain.errors.InvalidAdId

trait AdImageRepository[F[_]]:
  def create(image: Image): F[Unit]
  def find(imageId: ImageId): OptionT[F, Image]
  def findIdsByAd(adId: AdId): OptionT[F, List[ImageId]]
  def delete(imageId: ImageId): F[Unit]

object AdImageRepository:
  extension [F[_]: MonadThrow](repo: AdImageRepository[F])
    def get(imageId: ImageId): F[Image] =
      repo.find(imageId).getOrRaise(InvalidImageId(imageId))
    def getIdsByAd(adId: AdId): F[List[ImageId]] =
      repo.findIdsByAd(adId).getOrRaise(InvalidAdId(adId))
