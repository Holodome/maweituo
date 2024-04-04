package com.holodome.recs.config

import com.holodome.config.types.{CassandraConfig, HttpServerConfig, MinioConfig}

object types {
  case class RecsConfig(
      cassandra: CassandraConfig,
      minio: MinioConfig,
      recsServer: HttpServerConfig
  )
}
