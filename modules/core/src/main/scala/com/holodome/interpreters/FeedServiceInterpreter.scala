package com.holodome.interpreters

import scala.concurrent.duration.DurationInt

import com.holodome.domain.ads.AdId
import com.holodome.domain.pagination.Pagination
import com.holodome.domain.repos.FeedRepository
import com.holodome.domain.services.{ FeedService, RecommendationService }
import com.holodome.domain.users.UserId
import com.holodome.effects.TimeSource

import cats.MonadThrow
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

object FeedServiceInterpreter:
  def make[F[_]: MonadThrow: Logger: TimeSource](
      repo: FeedRepository[F],
      recs: RecommendationService[F]
  ): FeedService[F] = new:
    def getPersonalized(user: UserId, pag: Pagination): F[List[AdId]] =
      repo.getPersonalized(user, pag).flatMap {
        case List() =>
          for
            recs <- recs.getRecs(user, 100)
            _    <- repo.setPersonalized(user, recs, (60 * 60).seconds)
            r    <- repo.getPersonalized(user, pag)
            _    <- Logger[F].info(s"Generated feed for user $user")
          yield r
        case lst => lst.pure[F]
      }

    def getGlobal(pag: Pagination): F[List[AdId]] = repo.getGlobal(pag)
    def getGlobalSize: F[Int]                     = repo.getGlobalSize
    def getPersonalizedSize(user: UserId): F[Int] = repo.getPersonalizedSize(user)
