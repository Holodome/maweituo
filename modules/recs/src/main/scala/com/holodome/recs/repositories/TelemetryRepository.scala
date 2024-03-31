package com.holodome.recs.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait TelemetryRepository[F[_]] {
  def userClicked(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
}