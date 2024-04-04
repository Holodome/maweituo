package com.holodome.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import ciris.refined._
import com.comcast.ip4s._
import com.holodome.config.types._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import com.holodome.ext.ip4s.codecs._

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Config {
  def load[F[_]: Async]: F[AppConfig] =
    default[F].load[F]

  def default[F[_]]: ConfigValue[F, AppConfig] =
    (
      httpServerConfig,
      cassandraConfig,
      jwtConfig,
      redisConfig,
      minioConfig,
      recsServerConfig
    ).parMapN(AppConfig.apply)

  def cassandraConfig[F[_]]: ConfigValue[F, CassandraConfig] =
    (
      prop("cassandra.host").as[Host],
      prop("cassandra.port").as[Port],
      prop("cassandra.detacenter").as[NonEmptyString],
      env("MW_CASSANDRA_KEYSPACE")
    ).parMapN(CassandraConfig.apply)

  def httpServerConfig[F[_]]: ConfigValue[F, HttpServerConfig] =
    (
      prop("http.host").as[Host],
      prop("http.port").as[Port]
    ).parMapN(HttpServerConfig)

  def jwtConfig[F[_]]: ConfigValue[F, JwtConfig] =
    (
      prop("jwt.expire").as[JwtTokenExpiration],
      env("MW_JWT_SECRET_KEY").as[JwtAccessSecret].secret
    ).parMapN(JwtConfig.apply)

  def redisConfig[F[_]]: ConfigValue[F, RedisConfig] =
    (
      prop("redis.uri").as[Host]
    ).map(RedisConfig.apply)

  def minioConfig[F[_]]: ConfigValue[F, MinioConfig] =
    (
      prop("minio.host").as[Host],
      prop("minio.port").as[Port],
      env("MW_MINIO_USER").as[NonEmptyString].secret,
      env("MW_MINIO_PASSWORD").as[NonEmptyString].secret,
      prop("minio.bucket").as[NonEmptyString]
    ).parMapN(MinioConfig.apply)

  def recsServerConfig[F[_]]: ConfigValue[F, RecsClientConfig] =
    (
      prop("recs_client.timeout").as[FiniteDuration],
      prop("recs_client.idle_time_in_pool").as[FiniteDuration],
      prop("recs.host").as[Host],
      prop("recs.port").as[Port]
    ).parMapN { case (timeout, idle, host, port) =>
      RecsClientConfig(HttpClientConfig(timeout, idle), host, port)
    }
}
