package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.users.UserId

trait TelemetryService[F[_]] {
  def userCreated(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
}