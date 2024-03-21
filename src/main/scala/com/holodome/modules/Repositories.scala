package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories.UserRepository
import com.holodome.repositories.cassandra.CassandraRepositoryCreator

trait Repositories[F[_]] {
  val userRepository: UserRepository[F]
}

object Repositories {
  def make[F[_]: Async](cassandraConfig: CassandraConfig): Repositories[F] = {
    val cassandraRepositoryCreator = CassandraRepositoryCreator.make[F](cassandraConfig)
    new Repositories[F] {
      override val userRepository: UserRepository[F] = cassandraRepositoryCreator.userRepository()
    }
  }
}
