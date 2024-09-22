package com.holodome.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import ciris.http4s._
import ciris.refined._
import com.comcast.ip4s._
import com.holodome.config.types._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger

import java.nio.file.Paths
import utils.JsonConfig

object Config {
  private def recsServerConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, HttpServerConfig] =
    (
      file.stringField("recs.host").as[Host],
      file.stringField("recs.port").as[Port]
    ).parMapN(HttpServerConfig.apply)

  def loadAppConfig[F[_]: Async: Logger]: F[AppConfig] =
    env("MW_CONFIG_PATH")
      .load[F]
      .flatMap { path =>
        utils.JsonConfig
          .fromFile[F](Paths.get(path))
          .flatMap { implicit json =>
            defaultAppConfig[F].load[F]
          }
      }
      .onError { case e =>
        Logger[F].error(e)("Failed to load config")
      }

  private def defaultAppConfig[F[_]](implicit file: JsonConfig): ConfigValue[F, AppConfig] =
    (
      httpServerConfig,
      jwtConfig,
      redisConfig,
      minioConfig
    ).parMapN(AppConfig.apply)

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
}
