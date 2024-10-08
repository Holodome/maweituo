package maweituo.domain.ads.repos

import maweituo.domain.PaginatedCollection
import maweituo.domain.ads.*

trait AdSearchRepo[F[_]]:
  def search(req: AdSearchRequest): F[PaginatedCollection[AdId]]
