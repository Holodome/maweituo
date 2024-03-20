package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.cql.CassandraRepositories

trait Repositories[F[_]] {
  val userRepository: UserRepository[F]
}

object Repositories {
  def make[F[_]: Async](config: DatabaseConfig): Repositories[F] = {
    config match {
      case cfg @ CassandraConfig(_) => CassandraRepositories.make[F](cfg)
    }
  }
}
