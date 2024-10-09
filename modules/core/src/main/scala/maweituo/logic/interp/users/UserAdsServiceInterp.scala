package maweituo.logic.interp.users

import cats.MonadThrow

import maweituo.domain.ads.AdId
import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserAdsService

object UserAdsServiceInterp:
  def make[F[_]: MonadThrow](ads: AdRepo[F]): UserAdsService[F] = new:
    def getAds(userId: UserId): F[List[AdId]] = ads.findIdsByAuthor(userId)
