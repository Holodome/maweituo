package maweituo.logic.interp
import cats.MonadThrow

import maweituo.domain.PaginatedCollection
import maweituo.domain.ads.repos.AdSearchRepo
import maweituo.domain.ads.{AdId, AdSearchRequest}
import maweituo.domain.services.{FeedService, IAMService}
import maweituo.infrastructure.effects.TimeSource

import org.typelevel.log4cats.Logger

object FeedServiceInterp:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      adSearch: AdSearchRepo[F]
  )(using iam: IAMService[F]): FeedService[F] = new:
    def feed(req: AdSearchRequest): F[PaginatedCollection[AdId]] =
      adSearch.search(req)
