package maweituo
package modules

import cats.Parallel
import cats.effect.Async

import maweituo.domain.all.*
import maweituo.postgres.repos.all.*

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.LoggerFactory

sealed abstract class Repos[F[_]]:
  val users: UserRepo[F]
  val ads: AdRepo[F]
  val tags: AdTagRepo[F]
  val chats: ChatRepo[F]
  val messages: MessageRepo[F]
  val images: AdImageRepo[F]
  val telemetry: TelemetryRepo[F]
  val recs: RecsRepo[F]
  val adSearch: AdSearchRepo[F]

object Repositories:
  def makePostgres[F[_]: Async: LoggerFactory: Parallel](xa: Transactor[F]): Repos[F] = new:
    val users     = PostgresUserRepo.make[F](xa)
    val ads       = PostgresAdRepo.make[F](xa)
    val tags      = PostgresAdTagRepo.make[F](xa)
    val chats     = PostgresChatRepo.make[F](xa)
    val messages  = PostgresMessageRepo.make[F](xa)
    val images    = PostgresAdImageRepo.make[F](xa)
    val telemetry = PostgresTelemetryRepo.make[F](xa)
    val recs      = PostgresRecsRepo.make[F](xa)
    val adSearch  = PostgresAdSearchRepo.make[F](xa)
