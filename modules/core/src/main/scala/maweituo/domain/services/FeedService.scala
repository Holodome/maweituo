package maweituo
package domain
package services

import maweituo.domain.all.*

trait FeedService[F[_]]:
  def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]]
