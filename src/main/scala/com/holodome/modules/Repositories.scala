package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories.{JwtRepository, UserRepository}
import com.holodome.repositories.cassandra.CassandraResources
import com.holodome.repositories.redis.RedisJwtRepository
import dev.profunktor.redis4cats.RedisCommands

trait Repositories[F[_]] {
  val userRepository: UserRepository[F]
  val jwtRepository: JwtRepository[F]
}

object Repositories {
  def make[F[_]: Async](
      cassandraResources: CassandraResources[F],
      redis: RedisCommands[F, String, String],
      jwtTokenExpiration: JwtTokenExpiration
  ): Repositories[F] = {
    new Repositories[F] {
      override val userRepository: UserRepository[F] = cassandraResources.userRepository
      override val jwtRepository: JwtRepository[F] =
        RedisJwtRepository.make[F](redis, jwtTokenExpiration)
    }
  }
}
