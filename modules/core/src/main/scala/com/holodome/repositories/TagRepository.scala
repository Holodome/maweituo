package com.holodome.repositories

import com.holodome.domain.ads.{AdId, AdTag}

trait TagRepository[F[_]] {
  def getAllTags: F[List[AdTag]]
  def addTag(tag: AdTag): F[Unit]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[Set[AdId]]
}
