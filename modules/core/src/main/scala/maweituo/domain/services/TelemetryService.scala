package maweituo.domain.services

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId

trait TelemetryService[F[_]]:
  def userCreated(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
