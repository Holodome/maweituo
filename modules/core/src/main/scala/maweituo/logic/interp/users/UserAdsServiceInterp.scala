package maweituo
package logic
package interp
package users
import cats.MonadThrow

import maweituo.domain.all.*

object UserAdsServiceInterp:
  def make[F[_]: MonadThrow](ads: AdRepo[F]): UserAdsService[F] = new:
    def getAds(userId: UserId): F[List[AdId]] = ads.findIdsByAuthor(userId)
