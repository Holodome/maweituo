package com.holodome.config

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.FiniteDuration

import com.holodome.utils.Newtype
import com.holodome.utils.Wrapper
import com.holodome.utils.given

import cats.syntax.all.*
import ciris.ConfigDecoder
import ciris.Secret
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

type JwtAccessSecret = JwtAccessSecret.Type
object JwtAccessSecret extends Newtype[String]

type JwtTokenExpiration = JwtTokenExpiration.Type
object JwtTokenExpiration extends Newtype[FiniteDuration]

final case class JwtConfig(
    tokenExpiration: JwtTokenExpiration,
    accessSecret: Secret[JwtAccessSecret]
)

final case class AppConfig(
    httpServer: HttpServerConfig,
    jwt: JwtConfig,
    redis: RedisConfig,
    minio: MinioConfig
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
