package com.holodome.config

import ciris.Secret
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)

  @derive(configDecoder, show)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

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

  case class RecsConfig(
      cassandra: CassandraConfig,
      minio: MinioConfig,
      recsServer: HttpServerConfig,
      clickhouse: ClickHouseConfig
  )
}
