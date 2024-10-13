package maweituo

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}

import maweituo.config.Config
import maweituo.modules.*
import maweituo.resources.MkHttpServer

import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp.Simple:

  given LoggerFactory[IO] = Slf4jFactory.create[IO]
  private lazy val logger = LoggerFactory[IO].getLogger

  override def run: IO[Unit] =
    Config.loadAppConfig[IO] flatMap { cfg =>
      logger.info(s"Loaded config $cfg") >> Supervisor[IO](await = false).use { sp =>
        given Supervisor[IO] = sp
        AppResources
          .make[IO](cfg)
          .evalMap { res =>
            val repositories = Repositories.makePostgres[IO](res.postgres)
            for
              infrastructure <- Infrastructure.make[IO](cfg, res.redis, res.minio)
              services = Services.make[IO](repositories, infrastructure)
              api = HttpApi.make[IO](
                services
              )
            yield cfg.httpServer -> api.httpApp
          }
          .flatMap { case (cfg, httpApp) =>
            MkHttpServer[IO].newClient(cfg, httpApp)
          }
          .useForever
      }
    }
