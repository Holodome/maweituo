package com.holodome.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait RecommendationService[F[_]] {
  def getRecs(user: UserId): F[List[AdId]]

  def updateDbSnapshot(): F[Unit]
  def storeTelemetry(): F[Unit]
}
