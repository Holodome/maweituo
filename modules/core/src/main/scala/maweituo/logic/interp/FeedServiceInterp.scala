package maweituo
package logic
package interp

import maweituo.domain.all.*
import maweituo.infrastructure.effects.TimeSource

import org.typelevel.log4cats.Logger

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      adSearch: AdSearchRepo[F]
  )(using iam: IAMService[F]): FeedService[F] = new:
    def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]] =
      adSearch.search(req)
