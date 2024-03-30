package com.holodome.recs.resources

import cats.syntax.all._
import cats.effect.{Async, Resource}
import com.holodome.recs.config.types.RecsConfig
import com.holodome.resources.{MkCassandraClient, MkMinioClient}
import com.ringcentral.cassandra4io.CassandraSession
import io.minio.MinioAsyncClient

sealed abstract class RecsResources[F[_]](
    val cassandra: CassandraSession[F],
    val minio: MinioAsyncClient
)

object RecsResources {
  def make[F[_]: MkCassandraClient: MkMinioClient: Async](
      cfg: RecsConfig
  ): Resource[F, RecsResources[F]] =
    (
      MkCassandraClient[F].newClient(cfg.cassandra),
      MkMinioClient[F].newClient(cfg.minio)
    ).parMapN(new RecsResources(_, _) {})
}
