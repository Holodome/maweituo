package maweituo.domain.services

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.*
import maweituo.domain.users.UserId

trait AdImageService[F[_]]:
  def upload(uploader: UserId, adId: AdId, contents: ImageContentsStream[F]): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContentsStream[F]]
