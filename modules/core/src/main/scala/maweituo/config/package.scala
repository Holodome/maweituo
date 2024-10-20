package maweituo
package config

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.FiniteDuration

import cats.syntax.all.*

import maweituo.utils.{Newtype, Wrapper}

import ciris.{ConfigDecoder, Secret}
import com.comcast.ip4s.{Host, Port}

type JwtAccessSecret = JwtAccessSecret.Type
object JwtAccessSecret extends Newtype[String]

final case class JwtConfig(
    tokenExpiration: FiniteDuration,
    accessSecret: Secret[JwtAccessSecret]
)

final case class AppConfig(
    httpServer: HttpServerConfig,
    jwt: JwtConfig,
    redis: RedisConfig,
    minio: MinioConfig,
    postgres: PostgresConfig
)

final case class HttpClientConfig(
    timeout: FiniteDuration,
    idleTimeInPool: FiniteDuration
)

final case class RedisConfig(host: Host)

final case class MinioConfig(
    host: Host,
    port: Port,
    userId: Secret[String],
    password: Secret[String],
    bucket: String,
    url: String
)

final case class HttpServerConfig(
    host: Host,
    port: Port
)

final case class PostgresConfig(
    user: String,
    password: String,
    host: Host,
    port: Port,
    databaseName: String
)

export CirisOrphan.given

object CirisOrphan:
  given ConfigDecoder[String, Instant] =
    ConfigDecoder[String].mapOption("java.time.Instant")(s => Either.catchNonFatal(Instant.parse(s)).toOption)

  given ConfigDecoder[String, UUID] =
    ConfigDecoder[String].mapOption("java.util.UUID")(s => Either.catchNonFatal(UUID.fromString(s)).toOption)

  given [A, B](using
      wp: Wrapper[A, B],
      cd: ConfigDecoder[String, A]
  ): ConfigDecoder[String, B] =
    cd.map(a => wp.iso.get(a))
