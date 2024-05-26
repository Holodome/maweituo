package com.holodome.http

import com.holodome.domain.ads.AdId
import derevo.circe.magnolia.encoder
import derevo.derive

package object dto {
  @derive(encoder)
  final case class FeedDTO(ads: List[AdId], total: Int)
}
