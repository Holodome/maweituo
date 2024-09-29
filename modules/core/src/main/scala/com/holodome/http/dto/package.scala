package com.holodome.http.dto

import com.holodome.domain.ads.AdId

import io.circe.Codec

final case class FeedDTO(ads: List[AdId], total: Int) derives Codec.AsObject
