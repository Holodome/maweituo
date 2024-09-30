package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait UserAdsService[F[_]]:
  def getAds(userId: UserId): F[List[AdId]]
