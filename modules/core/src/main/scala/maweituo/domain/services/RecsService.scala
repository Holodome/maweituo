package maweituo.domain.services

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId

trait RecommendationService[F[_]]:
  def getRecs(user: UserId, count: Int): F[List[AdId]]
  def learn: F[Unit]
