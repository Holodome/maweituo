package com.holodome.rec

import com.holodome.domain.users.UserId

trait RecommendationAlgorithm[F[_]] {
  def get(user: UserId): F[List[UserId]]
}
