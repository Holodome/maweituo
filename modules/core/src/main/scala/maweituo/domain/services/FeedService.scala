package maweituo.domain.services

import maweituo.domain.PaginatedCollection
import maweituo.domain.ads.{AdId, AdSearchRequest}

trait FeedService[F[_]]:
  def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]]
