package com.holodome.domain.repositories

import cats.data.OptionT
import com.holodome.domain.ads.{AdId, AdTag}
import com.holodome.domain.users.UserId
import com.holodome.domain.recommendations.WeightVector

trait RecRepository[F[_]] {
  def userIsInRecs(user: UserId): F[Boolean]

  def get(userId: UserId): OptionT[F, WeightVector]
  def getClosest(user: UserId, count: Int): F[List[UserId]]

  def getUserCreated(user: UserId): OptionT[F, Set[AdId]]
  def getUserBought(user: UserId): OptionT[F, Set[AdId]]
  def getUserDiscussed(user: UserId): OptionT[F, Set[AdId]]

  def getTagByIdx(idx: Int): OptionT[F, AdTag]
  def getAdsByTag(tag: AdTag): OptionT[F, Set[AdId]]
}
