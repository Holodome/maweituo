package maweituo

import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import cats.syntax.all.*

import maweituo.config.Config
import maweituo.modules.*
import maweituo.resources.MkHttpServer

import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import zio.*
import zio.interop.catz.*

object MainZio extends CatsApp:
  given LoggerFactory[Task] = Slf4jFactory.create[Task]
  private lazy val logger   = LoggerFactory[Task].getLogger

  override def run =
    Config.loadAppConfig[Task].flatMap { cfg =>
      logger.info(s"Loaded config $cfg") >> Supervisor[Task](await = false).use { sp =>
        given Supervisor[Task] = sp
        AppResources
          .make[Task](cfg)
          .flatMap { res =>
            val repositories = Repositories.makePostgres[Task](res.postgres)
            for
              infrastructure <- Resource.eval(Infrastructure.make[Task](cfg, res.redis, res.minio))
              services = Services.make[Task](repositories, infrastructure)
              api = HttpApi.make[Task](
                cfg,
                services
              )
            yield cfg.httpServer -> api.httpApp
          }
          .flatMap { case (cfg, httpApp) =>
            MkHttpServer[Task].newClient(cfg, httpApp)
          }
          .useForever
      }
    }.exitCode
