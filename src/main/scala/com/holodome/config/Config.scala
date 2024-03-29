package com.holodome.config

import cats.effect.Async
import cats.syntax.all._
import ciris.refined._
import ciris._
import com.comcast.ip4s.IpLiteralSyntax
import com.holodome.config.types._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.cats._

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
    (
      env("MW_JWT_SECRET_KEY").as[JwtAccessSecret].secret,
      env("MW_MINIO_USER").as[NonEmptyString].secret,
      env("MW_MINIO_PASSWORD").as[NonEmptyString].secret,
      env("MW_CASSANDRA_KEYSPACE")
    ) parMapN { case (jwtAccessSecret, minioUser, minioPassword, cassandraKeyspace) =>
      AppConfig(
        HttpServerConfig(
          host"0.0.0.0",
          port"8080"
        ),
        CassandraConfig(host"localhost", port"9042", "datacenter1", cassandraKeyspace),
        JwtTokenExpiration(30.minutes),
        jwtAccessSecret,
        RedisConfig(RedisURI("redis://localhost")),
        MinioConfig("http://localhost:9000", minioUser, minioPassword, "maweituo")
      )
    }
  }
}
