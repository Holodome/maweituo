package com.holodome.modules

import cats.effect.Async
import com.holodome.repositories._
import com.ringcentral.cassandra4io.CassandraSession
import com.holodome.repositories.cassandra.{
  CassandraAdvertisementRepository,
  CassandraChatRepository,
  CassandraImageRepository,
  CassandraMessageRepository,
  CassandraUserRepository
}

trait Repositories[F[_]] {
  val users: UserRepository[F]
  val ads: AdvertisementRepository[F]
  val chats: ChatRepository[F]
  val messages: MessageRepository[F]
  val images: ImageRepository[F]
}

object Repositories {
  def make[F[_]: Async](cassandra: CassandraSession[F]): Repositories[F] = {
    new Repositories[F] {
      override val users: UserRepository[F]       = CassandraUserRepository.make[F](cassandra)
      override val messages: MessageRepository[F] = CassandraMessageRepository.make[F](cassandra)
      override val chats: ChatRepository[F]       = CassandraChatRepository.make[F](cassandra)
      override val ads: AdvertisementRepository[F] =
        CassandraAdvertisementRepository.make[F](cassandra)
      override val images: ImageRepository[F] = CassandraImageRepository.make[F](cassandra)
    }
  }
}
