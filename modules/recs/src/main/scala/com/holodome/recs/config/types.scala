package com.holodome.recs.config

import com.holodome.cassandra.config.CassandraConfig
import com.holodome.config.types.{HttpServerConfig, MinioConfig}

object types {
  case class ClickHouseConfig(
      jdbcUrl: String
  )

  case class RecsConfig(
      cassandra: CassandraConfig,
      minio: MinioConfig,
      recsServer: HttpServerConfig,
      clickhouse: ClickHouseConfig,
  )
}
