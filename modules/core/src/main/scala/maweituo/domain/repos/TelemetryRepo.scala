package maweituo.domain.repos

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId
import java.time.Instant

trait TelemetryRepo[F[_]]:
  def userViewed(user: UserId, ad: AdId, at: Instant): F[Unit]
  def userCreated(user: UserId, ad: AdId, at: Instant): F[Unit]
  def userBought(user: UserId, ad: AdId, at: Instant): F[Unit]
  def userDiscussed(user: UserId, ad: AdId, at: Instant): F[Unit]
