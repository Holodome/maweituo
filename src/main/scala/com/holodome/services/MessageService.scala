package com.holodome.services

import com.holodome.domain.users._
import com.holodome.domain.advertisements._

trait MessageService[F[_]] {
  def send(adId: AdvertisementId, from: UserId, to: UserId): F[Unit]
}


