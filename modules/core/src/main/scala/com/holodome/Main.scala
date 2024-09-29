package com.holodome

import com.holodome.config.Config
import com.holodome.domain.users.UserJwtAuth
import com.holodome.modules.*
import com.holodome.resources.MkHttpServer

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.log4cats.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pdi.jwt.JwtAlgorithm

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
            val recs         = RecsClients.make[IO]()
            for
              infrastructure <- Infrastructure.make[IO](cfg, res.redis, res.minio)
              services = Services.make[IO](repositories, infrastructure, recs)
              api = HttpApi.make[IO](
                services,
                UserJwtAuth(JwtAuth.hmac(cfg.jwt.accessSecret.value.value, JwtAlgorithm.HS256))
              )
            yield cfg.httpServer -> api.httpApp
          }
          .flatMap { case (cfg, httpApp) =>
            MkHttpServer[IO].newEmber(cfg, httpApp)
          }
          .useForever
      }
    }
