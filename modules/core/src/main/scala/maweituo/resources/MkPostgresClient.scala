package maweituo
package resources

import cats.effect.Resource

import maweituo.config.PostgresConfig

import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor

trait MkPostgresClient[F[_]]:
  def newClient(c: PostgresConfig): Resource[F, Transactor[F]]

object MkPostgresClient:
  def apply[F[_]: MkPostgresClient]: MkPostgresClient[F] = summon

  given [F[_]: Async]: MkPostgresClient[F] = new:
    def newClient(c: PostgresConfig): Resource[F, Transactor[F]] =
      for
        hikariConfig <- Resource.pure {
          val config = new HikariConfig()
          config.setDriverClassName("org.postgresql.Driver")
          config.setJdbcUrl(s"jdbc:postgresql://${c.host}:${c.port}/${c.databaseName}")
          config.setUsername(c.user)
          config.setPassword(c.password)
          config
        }
        xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
      yield xa
