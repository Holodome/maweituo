package com.holodome.recs.config

import com.holodome.config.types.CassandraConfig
import com.holodome.config.types.HttpServerConfig
import com.holodome.config.types.MinioConfig

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
