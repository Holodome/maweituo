package com.holodome.recs.resources

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.config.types.RecsConfig
import com.holodome.resources.{MkCassandraClient, MkClickHouseClient, MkMinioClient}
import com.ringcentral.cassandra4io.CassandraSession
import doobie.Transactor
import io.minio.MinioAsyncClient

sealed abstract class RecsResources[F[_]](
    val cassandra: CassandraSession[F],
    val minio: MinioAsyncClient,
    val clickhouse: Transactor[F]
)

object RecsResources {

  def make[F[_]: MkCassandraClient: MkMinioClient: MkClickHouseClient: Async](
      cfg: RecsConfig
  ): Resource[F, RecsResources[F]] =
    (
      MkCassandraClient[F].newClient(cfg.cassandra),
      MkMinioClient[F].newClient(cfg.minio),
      MkClickHouseClient[F].newClient(cfg.clickhouse)
    ).parMapN(new RecsResources(_, _, _) {})

}
