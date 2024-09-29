package com.holodome.domain.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag

trait TagRepository[F[_]]:
  def getAllTags: F[List[AdTag]]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[List[AdId]]
