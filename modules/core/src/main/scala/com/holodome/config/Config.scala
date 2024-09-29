package com.holodome.config

import java.nio.file.Paths

import com.holodome.config.*
import com.holodome.config.given
import com.holodome.config.utils.JsonConfig

import cats.effect.Async
import cats.syntax.all.*
import ciris.*
import ciris.http4s.*
import com.comcast.ip4s.*
import org.typelevel.log4cats.Logger

object Config:
  def loadAppConfig[F[_]: Async: Logger]: F[AppConfig] =
    env("MW_CONFIG_PATH")
      .load[F]
      .flatMap { path =>
        utils.JsonConfig
          .fromFile[F](Paths.get(path))
          .flatMap { json =>
            defaultAppConfig[F](using json).load[F]
          }
      }
      .onError { case e =>
        Logger[F].error(e)("Failed to load config")
      }

  private def defaultAppConfig[F[_]](using file: JsonConfig): ConfigValue[F, AppConfig] =
    (
      httpServerConfig[F],
      jwtConfig[F],
      redisConfig[F],
      minioConfig[F],
      postgresConfig[F]
    ).parMapN(AppConfig.apply)

  private def postgresConfig[F[_]](using file: JsonConfig): ConfigValue[F, PostgresConfig] =
    (
      file.stringField("postgres.user").as[String].covary[F],
      file.stringField("postgres.password").as[String].covary[F]
    ).parMapN(PostgresConfig.apply)

  private def httpServerConfig[F[_]](using file: JsonConfig): ConfigValue[F, HttpServerConfig] =
    (
      file.stringField("http.host").as[Host].covary[F],
      file.stringField("http.port").as[Port].covary[F]
    ).parMapN(HttpServerConfig.apply)

  private def jwtConfig[F[_]](using file: JsonConfig): ConfigValue[F, JwtConfig] =
    (
      file.stringField("jwt.expire").as[JwtTokenExpiration].covary[F],
      env("MW_JWT_SECRET_KEY").as[JwtAccessSecret].secret
    ).parMapN(JwtConfig.apply)

  private def redisConfig[F[_]](using file: JsonConfig): ConfigValue[F, RedisConfig] =
    file.stringField("redis.host").as[Host].map(RedisConfig.apply)

  private def minioConfig[F[_]](using file: JsonConfig): ConfigValue[F, MinioConfig] =
    (
      file.stringField("minio.host").as[Host],
      file.stringField("minio.port").as[Port],
      env("MW_MINIO_USER").as[String].secret,
      env("MW_MINIO_PASSWORD").as[String].secret,
      file.stringField("minio.bucket").as[String],
      file.stringField("minio.url").as[String]
    ).parMapN(MinioConfig.apply)
