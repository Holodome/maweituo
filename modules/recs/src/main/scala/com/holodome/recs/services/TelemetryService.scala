package com.holodome.recs.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait TelemetryService[F[_]] {
  def userClicked(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
}

object TelemetryService {
  def make[F[_]]: TelemetryService[F] =
    ???
}
