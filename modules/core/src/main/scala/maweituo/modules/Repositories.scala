package maweituo.modules

import cats.effect.kernel.Async

import maweituo.domain.ads.repos.*
import maweituo.domain.repos.*
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.*
import maweituo.postgres.repos.*
import maweituo.postgres.repos.users.*

import doobie.util.transactor.Transactor
import cats.NonEmptyParallel

sealed abstract class Repos[F[_]]:
  val users: UserRepo[F]
  val ads: AdRepo[F]
  val tags: AdTagRepo[F]
  val chats: ChatRepo[F]
  val messages: MessageRepo[F]
  val images: AdImageRepo[F]
  val feed: FeedRepo[F]
  val telemetry: TelemetryRepo[F]

object Repositories:
  def makePostgres[F[_]: Async: NonEmptyParallel](xa: Transactor[F]): Repos[F] = new:
    val users     = PostgresUserRepo.make[F](xa)
    val ads       = PostgresAdRepo.make[F](xa)
    val tags      = PostgresAdTagRepo.make[F](xa)
    val chats     = PostgresChatRepo.make[F](xa)
    val messages  = PostgresMessageRepo.make[F](xa)
    val images    = PostgresAdImageRepo.make[F](xa)
    val feed      = PostgresFeedRepo.make[F](xa)
    val telemetry = PostgresTelemetryRepo.make[F](xa)
