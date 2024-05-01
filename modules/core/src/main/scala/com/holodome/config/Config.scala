package com.holodome.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import ciris.http4s._
import ciris.refined._
import com.comcast.ip4s._
import com.holodome.config.types._
import com.holodome.ext.ciris.JsonConfig
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Uri

import java.nio.file.Paths
import scala.concurrent.duration.FiniteDuration

object Config {
  def load[F[_]: Async]: F[AppConfig] =
    env("MW_CONFIG_PATH").load[F].flatMap { path =>
      JsonConfig
        .fromFile[F](Paths.get(path))
        .flatMap { implicit json =>
          default[F].load[F]
        }
    }

  private def default[F[_]](implicit file: JsonConfig): ConfigValue[F, AppConfig] =
    (
      httpServerConfig,
      cassandraConfig,
      jwtConfig,
      redisConfig,
      minioConfig,
      recsClientConfig
    ).parMapN(AppConfig.apply)

  def cassandraConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, CassandraConfig] =
    (
      file.stringField("cassandra.host").as[Host],
      file.stringField("cassandra.port").as[Port],
      file.stringField("cassandra.datacenter").as[NonEmptyString],
      env("MW_CASSANDRA_KEYSPACE")
    ).parMapN(CassandraConfig.apply)

  private def httpServerConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, HttpServerConfig] =
    (
      file.stringField("http.host").as[Host],
      file.stringField("http.port").as[Port]
    ).parMapN(HttpServerConfig)

  private def jwtConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, JwtConfig] =
    (
      file.stringField("jwt.expire").as[JwtTokenExpiration],
      env("MW_JWT_SECRET_KEY").as[JwtAccessSecret].secret
    ).parMapN(JwtConfig.apply)

  private def redisConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, RedisConfig] =
    file.stringField("redis.host").as[Host].map(RedisConfig.apply)

  def minioConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, MinioConfig] =
    (
      file.stringField("minio.host").as[Host],
      file.stringField("minio.port").as[Port],
      env("MW_MINIO_USER").as[NonEmptyString].secret,
      env("MW_MINIO_PASSWORD").as[NonEmptyString].secret,
      file.stringField("minio.bucket").as[NonEmptyString],
      file.stringField("minio.url").as[NonEmptyString]
    ).parMapN(MinioConfig.apply)

  private def recsClientConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, RecsClientConfig] =
    (
      file.stringField("recs_client.timeout").as[FiniteDuration],
      file.stringField("recs_client.idle_time_in_pool").as[FiniteDuration],
      file.stringField("recs.uri").as[Uri],
      env("MW_NO_RECS").option.map(_.fold(false)(_ => true))
    ).parMapN { case (timeout, idle, uri, noRecs) =>
      RecsClientConfig(HttpClientConfig(timeout, idle), uri, noRecs)
    }
}
