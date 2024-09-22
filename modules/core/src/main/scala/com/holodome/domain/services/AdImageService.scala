package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.images.*
import com.holodome.domain.users.UserId

trait AdImageService[F[_]]:
  def upload(uploader: UserId, adId: AdId, contents: ImageContentsStream[F]): F[ImageId]
  def delete(imageId: ImageId, authenticated: UserId): F[Unit]
  def get(imageId: ImageId): F[ImageContentsStream[F]]
