package com.holodome.modules

import cats.effect.Async
import com.holodome.config.types._
import com.holodome.repositories._
import com.holodome.repositories.cassandra.{CassandraResources, CassandraUserRepository}

trait Repositories[F[_]] {
  val users: UserRepository[F]
  val ads: AdvertisementRepository[F]
  val chats: ChatRepository[F]
  val messages: MessageRepository[F]
  val images: ImageRepository[F]
}

object Repositories {
  def make[F[_]: Async](
      cassandraResources: CassandraResources
  ): Repositories[F] = {
    new Repositories[F] {
      override val users: UserRepository[F] =
        CassandraUserRepository.make[F](cassandraResources.userDb)
      override val messages: MessageRepository[F]  = ???
      override val chats: ChatRepository[F]        = ???
      override val ads: AdvertisementRepository[F] = ???
      override val images: ImageRepository[F]      = ???
    }
  }
}
