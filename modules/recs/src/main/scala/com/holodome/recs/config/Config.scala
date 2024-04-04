package com.holodome.recs.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import com.comcast.ip4s.{Host, Port}
import com.holodome.config.types.HttpServerConfig
import com.holodome.recs.config.types.RecsConfig
import com.holodome.ext.ip4s.codecs._

object Config {
  def load[F[_]: Async]: F[RecsConfig] =
    default[F].load[F]

  private def default[F[_]: Async]: ConfigValue[F, RecsConfig] =
    (
      com.holodome.config.Config.cassandraConfig,
      com.holodome.config.Config.minioConfig,
      recsServerConfig
    ).parMapN(RecsConfig.apply)

  def recsServerConfig[F[_]]: ConfigValue[F, HttpServerConfig] =
    (
      prop("recs.host").as[Host],
      prop("recs.port").as[Port]
    ).parMapN(HttpServerConfig.apply)
}
