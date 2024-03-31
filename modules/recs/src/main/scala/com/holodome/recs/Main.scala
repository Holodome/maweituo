package com.holodome.recs

import cats.effect.{IO, IOApp}
import cats.effect.std.Supervisor
import com.holodome.recs.config.Config
import com.holodome.recs.modules.{GRPCApi, Repositories, Services}
import com.holodome.recs.resources.RecsResources
import com.holodome.resources.MkHttpServer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.load[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { _ =>
          RecsResources
            .make[IO](cfg)
            .evalMap { res =>
              val repositories = Repositories.make[IO](res.cassandra)
              for {
                services <- Services.make[IO](repositories)
                api = GRPCApi.make[IO](services)
              } yield cfg.grpc -> api.httpApp
            }
            .flatMap { case (cfg, httpApp) =>
              MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }
}