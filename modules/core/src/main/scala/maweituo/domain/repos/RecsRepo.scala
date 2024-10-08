package maweituo.domain.repos

import maweituo.domain.users.UserId
import maweituo.domain.ads.AdId

trait RecsRepo[F[_]]:
  def getClosestAds(user: UserId, count: Int): F[List[AdId]]
  def learn: F[Unit]
