package com.holodome

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}
import com.holodome.config.Config
import com.holodome.modules.{HttpApi, Repositories, Services}
import com.holodome.resources.MkHttpServer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = {
    Config.load[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { implicit _ =>
          {
            val repositories = Repositories.make[IO](cfg.cassandraConfig)
            val services = Services.make[IO](repositories)
            val api = HttpApi.make[IO](services)
            MkHttpServer[IO]
              .newEmber(cfg.httpServerConfig, api.httpApp)
              .useForever
          }
        }
    }
  }
}
