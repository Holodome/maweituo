package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories._
import com.holodome.repositories.cassandra.{CassandraResources, CassandraUserRepository}
import com.holodome.repositories.redis.{RedisAuthedUserRepository, RedisJwtRepository}
import dev.profunktor.redis4cats.RedisCommands

trait Repositories[F[_]] {
  val userRepository: UserRepository[F]
  val jwtRepository: JwtRepository[F]
  val authedUserRepository: AuthedUserRepository[F]
  val advertisementRepository: AdvertisementRepository[F]
  val chatRepository: ChatRepository[F]
  val messageRepository: MessageRepository[F]
  val imageRepository: ImageRepository[F]
}

object Repositories {
  def make[F[_]: Async](
      cassandraResources: CassandraResources,
      redis: RedisCommands[F, String, String],
      jwtTokenExpiration: JwtTokenExpiration
  ): Repositories[F] = {
    new Repositories[F] {
      override val userRepository: UserRepository[F] =
        CassandraUserRepository.make[F](cassandraResources.userDb)
      override val jwtRepository: JwtRepository[F] =
        RedisJwtRepository.make[F](redis, jwtTokenExpiration)
      override val authedUserRepository: AuthedUserRepository[F] =
        RedisAuthedUserRepository.make[F](redis, jwtTokenExpiration)
      override val messageRepository: MessageRepository[F]             = ???
      override val chatRepository: ChatRepository[F]                   = ???
      override val advertisementRepository: AdvertisementRepository[F] = ???
      override val imageRepository: ImageRepository[F]                 = ???
    }
  }
}
