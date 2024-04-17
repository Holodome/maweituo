package com.holodome.recs.algo

import com.holodome.domain.users.UserId
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId

trait RecommendationAlgorithm[F[_]] {
  def obsIngest(obs: ObjectStorage[F], id: ObjectId): F[Unit]
  def getClosest(user: UserId, count: Int): F[List[UserId]]
}
