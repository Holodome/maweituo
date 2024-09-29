package com.holodome.domain.services

import com.holodome.domain.ads.AdId
import com.holodome.domain.ads.AdTag

trait AdTagService[F[_]]:
  def all: F[List[AdTag]]
  def find(tag: AdTag): F[List[AdId]]
