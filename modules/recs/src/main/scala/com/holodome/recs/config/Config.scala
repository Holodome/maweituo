package com.holodome.recs.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import com.comcast.ip4s.{Host, Port}
import com.holodome.config.types.HttpServerConfig
import com.holodome.ext.ciris.JsonConfig
import com.holodome.recs.config.types.{ClickHouseConfig, RecsConfig}
import ciris.http4s._

import java.nio.file.Paths

object Config {
  def load[F[_]: Async]: F[RecsConfig] =
    env("MW_CONFIG_PATH").load[F].flatMap { path =>
      JsonConfig
        .fromFile[F](Paths.get(path))
        .flatMap { implicit json =>
          default[F].load[F]
        }
    }

  private def default[F[_]: Async](implicit file: JsonConfig): ConfigValue[F, RecsConfig] =
    (
      com.holodome.config.Config.cassandraConfig,
      com.holodome.config.Config.minioConfig,
      recsServerConfig,
      clickhouseConfig
    ).parMapN(RecsConfig.apply)

  private def recsServerConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, HttpServerConfig] =
    (
      file.stringField("recs.host").as[Host],
      file.stringField("recs.port").as[Port]
    ).parMapN(HttpServerConfig.apply)

  private def clickhouseConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, ClickHouseConfig] =
    file.stringField("clickhouse.jdbcUrl").as[String].map(ClickHouseConfig.apply)
}
