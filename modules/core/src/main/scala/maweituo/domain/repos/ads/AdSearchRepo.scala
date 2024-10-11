package maweituo
package domain
package repos
package ads

import maweituo.domain.ads.*

trait AdSearchRepo[F[_]]:
  def search(req: AdSearchRequest): F[PaginatedCollection[AdId]]
