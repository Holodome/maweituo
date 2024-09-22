package com.holodome.config

import ciris.Secret
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import derevo.derive
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

package object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)

  @derive(configDecoder, show)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

  case class JwtConfig(
      tokenExpiration: JwtTokenExpiration,
      accessSecret: Secret[JwtAccessSecret]
  )

  case class AppConfig(
      httpServer: HttpServerConfig,
      cassandra: CassandraConfig,
      jwt: JwtConfig,
      redis: RedisConfig,
      minio: MinioConfig
  )
}
