package com.holodome.recs

import cats.effect.{IO, IOApp}
import com.holodome.config.Config
import com.holodome.recs.etl.{CassandraExtract, ClickhouseTransformLoad, RecETLInterpreter}
import com.holodome.recs.modules._
import com.holodome.recs.resources.RecsResources
import com.holodome.resources.MkHttpServer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.std.Supervisor

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.loadRecsConfig[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >> Supervisor[IO](await = false).use { implicit sp =>
        RecsResources
          .make[IO](cfg)
          .evalMap { res =>
            val repositories = Repositories.make[IO](res.cassandra, res.clickhouse)
            for {
              infrastructure <- Infrastructure.make[IO](cfg, res.minio)
              etl = RecETLInterpreter.make[IO](
                CassandraExtract.make[IO](res.cassandra),
                ClickhouseTransformLoad.make[IO](res.clickhouse),
                infrastructure.obs
              )
              services <- Services.make[IO](repositories, etl)
              api = GRPCApi.make[IO](services)
            } yield cfg.recsServer -> api.httpApp
          }
          .flatMap { case (cfg, httpApp) =>
            MkHttpServer[IO].newEmber(cfg, httpApp)
          }
          .useForever
      }
    }
}
