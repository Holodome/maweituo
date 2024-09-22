package com.holodome.http

import com.holodome.domain.ads.AdId

import io.circe.Codec

package object dto:
  final case class FeedDTO(ads: List[AdId], total: Int) derives Codec.AsObject
