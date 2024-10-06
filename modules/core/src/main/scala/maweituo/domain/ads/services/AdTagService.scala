package maweituo.domain.ads.services

import maweituo.domain.Identity
import maweituo.domain.ads.{AdId, AdTag}

trait AdTagService[F[_]]:
  def all: F[List[AdTag]]
  def find(tag: AdTag): F[List[AdId]]
  def addTag(id: AdId, tag: AdTag)(using Identity): F[Unit]
  def removeTag(id: AdId, tag: AdTag)(using Identity): F[Unit]
  def adTags(adId: AdId): F[List[AdTag]]
