package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait RecommendationService[F[_]] {
  def getRecs(user: UserId, count: Int): F[List[AdId]]
  def learn: F[Unit]
}
