package maweituo.domain.ads.repos

import cats.MonadThrow
import cats.data.OptionT

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.*
import maweituo.domain.errors.InvalidImageId

trait AdImageRepo[F[_]]:
  def create(image: Image): F[Unit]
  def find(imageId: ImageId): OptionT[F, Image]
  def findIdsByAd(adId: AdId): F[List[ImageId]]
  def delete(imageId: ImageId): F[Unit]

object AdImageRepo:
  extension [F[_]: MonadThrow](repo: AdImageRepo[F])
    def get(imageId: ImageId): F[Image] =
      repo.find(imageId).getOrRaise(InvalidImageId(imageId))