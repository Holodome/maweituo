package com.holodome.config

import com.comcast.ip4s.{Host, Port}
import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirisEnum, Enum, EnumEntry}

object types {
  sealed trait DatabaseConfig

  // Currently we support only local Cassandra installations with 1 server. OC this will be changed
  case class CassandraConfig(keyspace: String) extends DatabaseConfig

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class AppConfig(
      httpServerConfig: HttpServerConfig,
      databaseConfig: DatabaseConfig
  )

  sealed abstract class AppEnvironment extends EnumEntry with Lowercase

  object AppEnvironment
      extends Enum[AppEnvironment]
      with CirisEnum[AppEnvironment] {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    override def values: IndexedSeq[AppEnvironment] = findValues
  }

}
