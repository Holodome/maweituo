package maweituo
package domain
package services

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId

trait TelemetryService[F[_]]:
  def userViewed(user: UserId, ad: AdId): F[Unit]
  def userCreated(user: UserId, ad: AdId): F[Unit]
  def userBought(user: UserId, ad: AdId): F[Unit]
  def userDiscussed(user: UserId, ad: AdId): F[Unit]
