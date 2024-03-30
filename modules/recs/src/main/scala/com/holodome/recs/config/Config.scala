package com.holodome.recs.config

import cats.syntax.all._
import cats.effect.Async
import ciris.{env, ConfigValue}
import com.comcast.ip4s.IpLiteralSyntax
import com.holodome.config.types.{CassandraConfig, HttpServerConfig, MinioConfig}
import com.holodome.recs.config.types.RecsConfig
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.cats._
import ciris.refined._

object Config {
  def load[F[_]: Async]: F[RecsConfig] =
    default[F].load[F]

  private def default[F[_]: Async]: ConfigValue[F, RecsConfig] =
    (
      env("MW_CASSANDRA_KEYSPACE"),
      env("MW_MINIO_USER").as[NonEmptyString].secret,
      env("MW_MINIO_PASSWORD").as[NonEmptyString].secret
    )
      .parMapN { case (cassandraKeyspace, minioUser, minioPassword) =>
        RecsConfig(
          CassandraConfig(host"localhost", port"9042", "datacenter1", cassandraKeyspace),
          MinioConfig("http://localhost:9000", minioUser, minioPassword, "maweituo"),
          HttpServerConfig(host"0.0.0.0", port"11221")
        )
      }
}
