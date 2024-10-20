package maweituo
package config

import java.nio.file.Paths

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

import cats.Show
import cats.effect.Async
import cats.syntax.all.*

import maweituo.config.*
import maweituo.config.given
import maweituo.config.utils.JsonConfig

import ciris.*
import ciris.http4s.*
import com.comcast.ip4s.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

object Config:
  enum ConfigSource:
    case ConfigEnv(string: String)
    case ConfigTest
    case ConfigDefault

  def loadAppConfig[F[_]: Async: LoggerFactory]: F[AppConfig] =
    given Logger[F] = LoggerFactory[F].getLogger
    env("MW_CONFIG_PATH").map(ConfigSource.ConfigEnv.apply)
      .or(env("MW_TEST_CONFIG").map(_ => ConfigSource.ConfigTest))
      .default(ConfigSource.ConfigDefault)
      .load[F]
      .flatMap { path =>
        path match
          case ConfigSource.ConfigEnv(path) =>
            utils.JsonConfig.fromFile[F](Paths.get(path))
          case ConfigSource.ConfigTest =>
            utils.JsonConfig.fromString(Source.fromResource("maweituo-config-test.json").mkString)
          case ConfigSource.ConfigDefault =>
            utils.JsonConfig.fromString(Source.fromResource("maweituo-config.json").mkString)
      }.flatMap { json =>
        defaultAppConfig[F](using json).load[F]
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
      file.stringField("postgres.password").as[String].covary[F],
      file.stringField("postgres.host").as[Host].covary[F],
      file.stringField("postgres.port").as[Port].covary[F],
      file.stringField("postgres.db").as[String].covary[F]
    ).parMapN(PostgresConfig.apply)

  private def httpServerConfig[F[_]](using file: JsonConfig): ConfigValue[F, HttpServerConfig] =
    (
      file.stringField("http.host").as[Host].covary[F],
      file.stringField("http.port").as[Port].covary[F]
    ).parMapN(HttpServerConfig.apply)

  private def jwtConfig[F[_]](using file: JsonConfig): ConfigValue[F, JwtConfig] =
    (
      file.stringField("jwt.expire").as[FiniteDuration].covary[F],
      env("MW_JWT_SECRET_KEY").default("123").as[JwtAccessSecret].secret
    ).parMapN(JwtConfig.apply)

  private def redisConfig[F[_]](using file: JsonConfig): ConfigValue[F, RedisConfig] =
    file.stringField("redis.host").as[Host].map(RedisConfig.apply)

  private def minioConfig[F[_]](using file: JsonConfig): ConfigValue[F, MinioConfig] =
    (
      file.stringField("minio.host").as[Host],
      file.stringField("minio.port").as[Port],
      env("MW_MINIO_USER").default("minioadmin").as[String].secret,
      env("MW_MINIO_PASSWORD").default("minioadmin").as[String].secret,
      file.stringField("minio.bucket").as[String],
      file.stringField("minio.url").as[String]
    ).parMapN(MinioConfig.apply)
