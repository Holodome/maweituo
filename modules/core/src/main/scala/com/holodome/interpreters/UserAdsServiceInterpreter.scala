package com.holodome.interpreters

import com.holodome.domain.ads.AdId
import com.holodome.domain.repositories.AdRepository
import com.holodome.domain.services.UserAdsService
import com.holodome.domain.users.UserId

import cats.MonadThrow

object UserAdsServiceInterpreter:
  def make[F[_]: MonadThrow](ads: AdRepository[F]): UserAdsService[F] = new:
    def getAds(userId: UserId): F[List[AdId]] = ads.findIdsByAuthor(userId)
