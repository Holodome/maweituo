package com.holodome

import cats.effect.{IO, IOApp}
import cats.effect.std.Supervisor
import com.holodome.config.Config
import com.holodome.domain.users.UserJwtAuth
import com.holodome.http.ApplicationErrorHandler._
import com.holodome.modules._
import com.holodome.resources.MkHttpServer
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pdi.jwt.JwtAlgorithm

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = {
    Config.load[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >> Supervisor[IO](await = false).use { implicit sp =>
        AppResources
          .make[IO](cfg)
          .evalMap { res =>
            val repositories = Repositories.makeCassandra[IO](res.cassandra)
            val recs         = RecsClients.make[IO](res.grpcClient, cfg.recs)
            for {
              infrastructure <- Infrastructure.make[IO](cfg, res.redis, res.minio)
              services = Services.make[IO](repositories, infrastructure, recs)
              api = HttpApi.make[IO](
                services,
                UserJwtAuth(JwtAuth.hmac(cfg.jwt.accessSecret.value.value, JwtAlgorithm.HS256))
              )
            } yield cfg.httpServer -> api.httpApp
          }
          .flatMap { case (cfg, httpApp) =>
            MkHttpServer[IO]
              .newEmber(cfg, httpApp)
          }
          .useForever
      }
    }
  }
}
