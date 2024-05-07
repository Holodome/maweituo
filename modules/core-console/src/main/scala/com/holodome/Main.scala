package com.holodome

import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}
import com.holodome.config.Config
import com.holodome.modules._
import dev.profunktor.redis4cats.effect.MkRedis._
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.loadAppConfig[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >> Supervisor[IO](await = false).use { implicit sp =>
        AppResources
          .make[IO](cfg)
          .evalMap { res =>
            val repositories = Repositories.makeCassandra[IO](res.cassandra)
            val recs         = RecsClients.make[IO](res.grpcClient, cfg.recs)
            for {
              infrastructure <- Infrastructure.make[IO](cfg, res.redis, res.minio)
              services = Services.make[IO](repositories, infrastructure, recs)
            } yield ConsoleApi.make[IO](services)
          }
          .flatMap { api => Resource.make(api.run)(_ => IO.unit) }
          .useForever
      }
    }
}
