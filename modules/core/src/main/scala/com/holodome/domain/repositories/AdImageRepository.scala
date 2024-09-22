package com.holodome.domain.repositories

import com.holodome.domain.errors.InvalidImageId
import com.holodome.domain.images.*

import cats.MonadThrow
import cats.data.OptionT

trait AdImageRepository[F[_]]:
  def create(image: Image): F[Unit]
  def findMeta(imageId: ImageId): OptionT[F, Image]
  def delete(imageId: ImageId): F[Unit]

object AdImageRepository:
  extension [F[_]: MonadThrow](repo: AdImageRepository[F])
    def getMeta(imageId: ImageId): F[Image] =
      repo.findMeta(imageId).getOrRaise(InvalidImageId(imageId))
