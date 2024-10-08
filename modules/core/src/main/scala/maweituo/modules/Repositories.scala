package maweituo.modules

import cats.NonEmptyParallel
import cats.effect.kernel.Async

import maweituo.domain.ads.repos.*
import maweituo.domain.repos.*
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.*
import maweituo.postgres.repos.*
import maweituo.postgres.repos.users.*

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

sealed abstract class Repos[F[_]]:
  val users: UserRepo[F]
  val ads: AdRepo[F]
  val tags: AdTagRepo[F]
  val chats: ChatRepo[F]
  val messages: MessageRepo[F]
  val images: AdImageRepo[F]
  val telemetry: TelemetryRepo[F]
  val recs: RecsRepo[F]

object Repositories:
  def makePostgres[F[_]: Async: NonEmptyParallel: Logger](xa: Transactor[F]): Repos[F] = new:
    val users     = PostgresUserRepo.make[F](xa)
    val ads       = PostgresAdRepo.make[F](xa)
    val tags      = PostgresAdTagRepo.make[F](xa)
    val chats     = PostgresChatRepo.make[F](xa)
    val messages  = PostgresMessageRepo.make[F](xa)
    val images    = PostgresAdImageRepo.make[F](xa)
    val telemetry = PostgresTelemetryRepo.make[F](xa)
    val recs      = PostgresRecsRepo.make[F](xa)
