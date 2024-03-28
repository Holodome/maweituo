package com.holodome.config

import cats.effect.Async
import ciris._
import com.comcast.ip4s.IpLiteralSyntax
import com.holodome.config.types._

import scala.concurrent.duration.DurationInt

object Config {
  def load[F[_]: Async]: F[AppConfig] =
    env("MW_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case AppEnvironment.Test => default[F]
        case AppEnvironment.Prod => ???
      }
      .load[F]

  private def default[F[_]]: ConfigValue[F, AppConfig] = {
    env("MW_JWT_SECRET_KEY").as[JwtAccessSecret].secret map { jwtAccessSecret =>
      AppConfig(
        HttpServerConfig(
          host"0.0.0.0",
          port"8080"
        ),
        CassandraConfig(host"localhost", port"9042", "datacenter1", "maweituo"),
        JwtTokenExpiration(30.minutes),
        jwtAccessSecret,
        RedisConfig(RedisURI("redis://localhost"))
      )
    }
  }
}
