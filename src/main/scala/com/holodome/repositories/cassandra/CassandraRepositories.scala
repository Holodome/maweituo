package com.holodome.repositories.cassandra

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.modules.Repositories
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.outworkers.phantom.connectors

object CassandraRepositories {
  def make[F[_]: Async](config: CassandraConfig): CassandraRepositories[F] =
    new CassandraRepositories[F](config) {}
}

sealed abstract class CassandraRepositories[F[_]: Async] private (
    config: CassandraConfig
) extends Repositories[F] {
  private val connector =
    connectors.ContactPoint.local.keySpace(config.keyspace)
  private val userDb = new UsersDatabase(connector)

  override val userRepository: UserRepository[F] =
    CassandraUserRepository.make(userDb)
}
