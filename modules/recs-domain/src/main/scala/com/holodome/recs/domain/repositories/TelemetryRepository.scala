package com.holodome.recs.domain.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait TelemetryRepository[F[_]] {
  def userCreated(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
}
