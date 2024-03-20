package com.holodome.config

import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, default, env}
import com.comcast.ip4s.IpLiteralSyntax
import com.holodome.config.types._
import com.holodome.config.types.{AppConfig, HttpServerConfig}

object Config {
  def load[F[_]: Async]: F[AppConfig] =
    env("MW_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case AppEnvironment.Test => ???
        case AppEnvironment.Prod => ConfigValue.default(default)
      }
      .load[F]

  private def default: AppConfig =
    AppConfig(
      HttpServerConfig(
        host"0.0.0.0",
        port"8080"
      ),
      CassandraConfig("maweituo")
    )
}
