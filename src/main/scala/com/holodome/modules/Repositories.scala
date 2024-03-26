package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories._
import com.holodome.repositories.cassandra.{CassandraResources, CassandraUserRepository}

trait Repositories[F[_]] {
  val userRepository: UserRepository[F]
  val advertisementRepository: AdvertisementRepository[F]
  val chatRepository: ChatRepository[F]
  val messageRepository: MessageRepository[F]
  val imageRepository: ImageRepository[F]
}

object Repositories {
  def make[F[_]: Async](
      cassandraResources: CassandraResources
  ): Repositories[F] = {
    new Repositories[F] {
      override val userRepository: UserRepository[F] =
        CassandraUserRepository.make[F](cassandraResources.userDb)
      override val messageRepository: MessageRepository[F]             = ???
      override val chatRepository: ChatRepository[F]                   = ???
      override val advertisementRepository: AdvertisementRepository[F] = ???
      override val imageRepository: ImageRepository[F]                 = ???
    }
  }
}
