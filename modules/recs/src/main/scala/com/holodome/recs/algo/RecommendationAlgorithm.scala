package com.holodome.recs.algo

import com.holodome.domain.users.UserId

trait RecommendationAlgorithm[F[_]] {
  def get(user: UserId): F[List[UserId]]
}
