package com.holodome.config

import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import derevo.derive
import enumeratum.{CirisEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

  @newtype case class RedisURI(value: String)
  @newtype case class RedisConfig(uri: RedisURI)

  case class CassandraConfig(host: Host, port: Port, datacenter: String, keyspace: String)
  case class MinioConfig(endpoint: String, userId: String, password: String, bucket: String)

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class AppConfig(
      httpServer: HttpServerConfig,
      cassandra: CassandraConfig,
      jwtTokenExpiration: JwtTokenExpiration,
      jwtAccessSecret: Secret[JwtAccessSecret],
      redis: RedisConfig,
      minio: MinioConfig
  )

  sealed abstract class AppEnvironment extends EnumEntry with Lowercase

  object AppEnvironment extends Enum[AppEnvironment] with CirisEnum[AppEnvironment] {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    override def values: IndexedSeq[AppEnvironment] = findValues
  }

  case class InvalidConfig()

}
