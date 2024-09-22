package com.holodome.domain.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

import cats.data.OptionT

trait UserAdsRepository[F[_]]:
  def create(userId: UserId, ad: AdId): F[Unit]
  def get(userId: UserId): OptionT[F, Set[AdId]]
