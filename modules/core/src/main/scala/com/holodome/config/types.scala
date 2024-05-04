package com.holodome.config

import ciris.Secret
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import derevo.derive
import eu.timepit.refined.types.all.NonEmptyString
import io.estatico.newtype.macros.newtype
import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)

  @derive(configDecoder, show)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

  @newtype case class RedisConfig(host: Host)

  case class CassandraConfig(host: Host, port: Port, datacenter: NonEmptyString, keyspace: String)
  case class MinioConfig(
      host: Host,
      port: Port,
      userId: Secret[NonEmptyString],
      password: Secret[NonEmptyString],
      bucket: NonEmptyString,
      url: NonEmptyString
  )

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

  case class RecsClientConfig(
      client: HttpClientConfig,
      uri: Uri,
      noRecs: Boolean
  )

  case class JwtConfig(
      tokenExpiration: JwtTokenExpiration,
      accessSecret: Secret[JwtAccessSecret]
  )

  case class AppConfig(
      httpServer: HttpServerConfig,
      cassandra: CassandraConfig,
      jwt: JwtConfig,
      redis: RedisConfig,
      minio: MinioConfig,
      recs: RecsClientConfig
  )
}
