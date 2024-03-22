package com.holodome.config

import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import derevo.derive
import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirisEnum, Enum, EnumEntry}
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration
import com.holodome.ext.ciris.configDecoder
import derevo.cats.show
import dev.profunktor.auth.jwt.JwtAuth

object types {
  @derive(configDecoder, show)
  @newtype case class JwtAccessSecret(value: String)
  @newtype case class JwtTokenExpiration(value: FiniteDuration)

  @newtype case class RedisURI(value: String)
  @newtype case class RedisConfig(uri: RedisURI)

  // Currently we support only local Cassandra installations with 1 server. OC this will be changed
  case class CassandraConfig(keyspace: String)

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class AppConfig(
      httpServerConfig: HttpServerConfig,
      cassandraConfig: CassandraConfig,
      jwtTokenExpiration: JwtTokenExpiration,
      jwtAccessSecret: Secret[JwtAccessSecret],
      redisConfig: RedisConfig
  )

  sealed abstract class AppEnvironment extends EnumEntry with Lowercase

  object AppEnvironment extends Enum[AppEnvironment] with CirisEnum[AppEnvironment] {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    override def values: IndexedSeq[AppEnvironment] = findValues
  }

}
