package maweituo

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}

import maweituo.config.Config
import maweituo.modules.*
import maweituo.resources.MkHttpServer

import dev.profunktor.redis4cats.log4cats.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.loadAppConfig[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >> Supervisor[IO](await = false).use { sp =>
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
