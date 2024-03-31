package com.holodome.recs.algo

import com.holodome.domain.users.UserId

trait RecommendationAlgorithm[F[_]] {
  def getClosest(user: UserId): F[List[UserId]]
}
