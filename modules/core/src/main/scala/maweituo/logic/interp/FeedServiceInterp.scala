package maweituo
package logic
package interp

import cats.MonadThrow

import maweituo.domain.all.*
import maweituo.infrastructure.effects.TimeSource

object FeedServiceInterp:
  def make[F[_]: MonadThrow: TimeSource](
      adSearch: AdSearchRepo[F]
  )(using iam: IAMService[F]): FeedService[F] = new:
    def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]] =
      adSearch.search(req)
