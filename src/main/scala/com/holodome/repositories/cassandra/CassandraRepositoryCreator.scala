package com.holodome.repositories.cassandra

import cats.effect.Async
import com.holodome.config.types.CassandraConfig
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.cql.UsersDatabase
import com.outworkers.phantom.connectors

trait CassandraRepositoryCreator[F[_]] {
  def userRepository(): UserRepository[F]
}

object CassandraRepositoryCreator {
  def make[F[_]: Async](config: CassandraConfig): CassandraRepositoryCreator[F] =
    new CassandraRepositoryCreator[F] {
      private val connector =
        connectors.ContactPoint.local.keySpace(config.keyspace)
      private val userDb = new UsersDatabase(connector)

      override def userRepository(): UserRepository[F] = CassandraUserRepository.make[F](userDb)
    }
}
