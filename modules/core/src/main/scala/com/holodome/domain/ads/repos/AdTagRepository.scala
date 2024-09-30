package com.holodome.domain.ads.repos

import com.holodome.domain.ads.{ AdId, AdTag }

trait AdTagRepository[F[_]]:
  def getAllTags: F[List[AdTag]]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def getAdTags(adId: AdId): F[List[AdTag]]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[List[AdId]]
