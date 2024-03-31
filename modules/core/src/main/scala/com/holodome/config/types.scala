package com.holodome.config

import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import derevo.derive
import eu.timepit.refined.types.all.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

  @newtype case class RedisURI(value: String)
  @newtype case class RedisConfig(uri: RedisURI)

  case class CassandraConfig(host: Host, port: Port, datacenter: String, keyspace: String)
  case class MinioConfig(
      endpoint: String,
      userId: Secret[NonEmptyString],
      password: Secret[NonEmptyString],
      bucket: String
  )

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

  case class GrpcConfig(
      client: HttpClientConfig,
      host: Host,
      port: Port
  )

  case class AppConfig(
      httpServer: HttpServerConfig,
      cassandra: CassandraConfig,
      jwtTokenExpiration: JwtTokenExpiration,
      jwtAccessSecret: Secret[JwtAccessSecret],
      redis: RedisConfig,
      minio: MinioConfig,
      grpc: GrpcConfig
  )
}
