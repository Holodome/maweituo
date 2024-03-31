package com.holodome.recs.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait TelemetryRepository[F[_]] {
  def userClicked(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]

  def getUserClicked(user: UserId): F[Set[AdId]]
  def getUserBought(user: UserId): F[Set[AdId]]
  def getUserDiscussed(user: UserId): F[Set[AdId]]
}
