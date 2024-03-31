package com.holodome.recs.repositories

import cats.data.OptionT
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.users.UserId
import com.holodome.recs.domain.recommendations.WeightVector

trait RecRepository[F[_]] {
  def insert(userId: UserId, weights: WeightVector): F[Unit]
  def get(userId: UserId): OptionT[F, WeightVector]

  def getTagByIdx(idx: Int): OptionT[F, AdTag]
  def getAdsByTag(tag: AdTag, limit: Int): F[List[AdId]]
}
