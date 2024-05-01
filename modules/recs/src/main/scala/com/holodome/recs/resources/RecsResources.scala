package com.holodome.recs.resources

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.recs.config.types.{ClickHouseConfig, RecsConfig}
import com.holodome.resources.{MkCassandraClient, MkMinioClient}
import com.ringcentral.cassandra4io.CassandraSession
import doobie.Transactor
import io.minio.MinioAsyncClient

import java.util.Properties

sealed abstract class RecsResources[F[_]](
    val cassandra: CassandraSession[F],
    val minio: MinioAsyncClient,
    val clickhouse: Transactor[F]
)

object RecsResources {

  private def clickhouseTransactor[F[_]: Async](cfg: ClickHouseConfig): Transactor[F] = {
    val properties = {
      val p = new Properties()
      p.setProperty("http_connection_provider", "HTTP_URL_CONNECTION")
      p
    }
    Transactor.fromDriverManager[F](
      driver = "com.clickhouse.jdbc.ClickHouseDriver",
      url = cfg.jdbcUrl,
      logHandler = None,
      info = properties
    )
  }

  def make[F[_]: MkCassandraClient: MkMinioClient: Async](
      cfg: RecsConfig
  ): Resource[F, RecsResources[F]] =
    (
      MkCassandraClient[F].newClient(cfg.cassandra),
      MkMinioClient[F].newClient(cfg.minio)
    ).parMapN(
      new RecsResources(
        _,
        _,
        clickhouseTransactor(cfg.clickhouse)
      ) {}
    )
}
