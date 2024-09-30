package com.holodome.domain.services

import com.holodome.domain.ads.{ AdId, AdTag }
import com.holodome.domain.users.UserId

trait AdTagService[F[_]]:
  def all: F[List[AdTag]]
  def find(tag: AdTag): F[List[AdId]]
  def addTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
  def removeTag(id: AdId, tag: AdTag, userId: UserId): F[Unit]
