package com.holodome.resources

import com.holodome.config.ClickHouseConfig
import cats.effect.Resource
import doobie.util.transactor.Transactor
import java.util.Properties
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import cats.effect.kernel.Async

trait MkClickHouseClient[F[_]] {
  def newClient(c: ClickHouseConfig): Resource[F, Transactor[F]]
}

object MkClickHouseClient {
  def apply[F[_]: MkClickHouseClient]: MkClickHouseClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: MkClickHouseClient[F] =
    new MkClickHouseClient[F] {
      def newClient(cfg: ClickHouseConfig): Resource[F, Transactor[F]] = {
        val properties = {
          val p = new Properties()
          p.setProperty("http_connection_provider", "HTTP_URL_CONNECTION")
          p
        }
        for {
          hikariConfig <- Resource.pure {
            val config = new HikariConfig()
            config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver")
            config.setJdbcUrl(cfg.jdbcUrl)
            config.setDataSourceProperties(properties)
            config
          }
          xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
        } yield xa
      }
    }
}
