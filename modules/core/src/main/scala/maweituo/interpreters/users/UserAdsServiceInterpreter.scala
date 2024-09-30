package maweituo.interpreters.users

import maweituo.domain.ads.AdId
import maweituo.domain.ads.repos.AdRepository
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserAdsService

import cats.MonadThrow

object UserAdsServiceInterpreter:
  def make[F[_]: MonadThrow](ads: AdRepository[F]): UserAdsService[F] = new:
    def getAds(userId: UserId): F[List[AdId]] = ads.findIdsByAuthor(userId)
