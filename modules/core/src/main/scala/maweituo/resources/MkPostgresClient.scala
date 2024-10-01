package maweituo.resources

import maweituo.config.PostgresConfig

import cats.effect.{Async, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor

trait MkPostgresClient[F[_]]:
  def newClient(c: PostgresConfig): Resource[F, Transactor[F]]

object MkPostgresClient:
  def apply[F[_]: MkPostgresClient]: MkPostgresClient[F] = summon

  private def makeTransactor[F[_]: Async](c: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for
      hikariConfig <- Resource.pure {
        val config = new HikariConfig()
        config.setDriverClassName("org.postgresql.Driver")
        config.setJdbcUrl(f"jdbc:postgresql://${c.host}:${c.port}/${c.databaseName}")
        config.setUsername(c.user)
        config.setPassword(c.password)
        config
      }
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    yield xa

  given [F[_]: Async]: MkPostgresClient[F] = new:
    def newClient(c: PostgresConfig): Resource[F, Transactor[F]] =
      makeTransactor[F](c)
