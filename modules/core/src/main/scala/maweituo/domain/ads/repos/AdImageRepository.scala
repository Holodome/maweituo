package maweituo.domain.ads.repos

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.*
import maweituo.domain.errors.InvalidImageId

import cats.MonadThrow
import cats.data.OptionT

trait AdImageRepository[F[_]]:
  def create(image: Image): F[Unit]
  def find(imageId: ImageId): OptionT[F, Image]
  def findIdsByAd(adId: AdId): F[List[ImageId]]
  def delete(imageId: ImageId): F[Unit]

object AdImageRepository:
  extension [F[_]: MonadThrow](repo: AdImageRepository[F])
    def get(imageId: ImageId): F[Image] =
      repo.find(imageId).getOrRaise(InvalidImageId(imageId))
