package maweituo.domain.ads.repos

import maweituo.domain.ads.{AdId, AdTag}

trait AdTagRepo[F[_]]:
  def getAllTags: F[List[AdTag]]
  def addTagToAd(adId: AdId, tag: AdTag): F[Unit]
  def getAdTags(adId: AdId): F[List[AdTag]]
  def removeTagFromAd(adId: AdId, tag: AdTag): F[Unit]
  def getAllAdsByTag(tag: AdTag): F[List[AdId]]
