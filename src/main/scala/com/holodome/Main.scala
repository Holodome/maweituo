package com.holodome

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}
import com.holodome.config.Config
import com.holodome.modules.{HttpApi, Repositories, Services}
import com.holodome.resources.{AppResources, MkHttpServer}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect._
import cats.effect.std.Supervisor
import com.holodome.domain.users.UserJwtAuth
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtAlgorithm

object Main extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = {
    Config.load[IO] flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { _ =>
          AppResources
            .make[IO](cfg)
            .evalMap { res =>
              val repositories =
                Repositories.make[IO](res.cassandra, res.redis, cfg.jwtTokenExpiration)
              for {
                services <- Services.make[IO](repositories, cfg)
                api = HttpApi.make[IO](
                  services,
                  UserJwtAuth(JwtAuth.hmac(cfg.jwtAccessSecret.value.value, JwtAlgorithm.HS256))
                )
              } yield cfg.httpServerConfig -> api.httpApp
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
