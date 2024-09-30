package maweituo.domain.users.services

import maweituo.domain.ads.AdId
import maweituo.domain.users.UserId

trait UserAdsService[F[_]]:
  def getAds(userId: UserId): F[List[AdId]]
