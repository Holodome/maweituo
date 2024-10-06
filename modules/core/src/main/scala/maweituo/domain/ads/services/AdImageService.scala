package maweituo.domain.services

import maweituo.domain.Identity
import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.*

trait AdImageService[F[_]]:
  def upload(adId: AdId, contents: ImageContentsStream[F])(using Identity): F[ImageId]
  def delete(imageId: ImageId)(using Identity): F[Unit]
  def get(imageId: ImageId): F[ImageContentsStream[F]]
