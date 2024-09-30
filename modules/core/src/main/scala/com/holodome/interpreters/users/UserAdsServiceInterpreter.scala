package com.holodome.interpreters.users

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.repos.AdRepository
import com.holodome.domain.users.UserId
import com.holodome.domain.users.services.UserAdsService

import cats.MonadThrow

object UserAdsServiceInterpreter:
  def make[F[_]: MonadThrow](ads: AdRepository[F]): UserAdsService[F] = new:
    def getAds(userId: UserId): F[List[AdId]] = ads.findIdsByAuthor(userId)
