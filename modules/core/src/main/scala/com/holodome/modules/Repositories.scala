package com.holodome.modules

import cats.effect.Async
import com.holodome.repositories._
import com.holodome.repositories.cassandra._
import com.ringcentral.cassandra4io.CassandraSession

sealed abstract class Repositories[F[_]] {
  val users: UserRepository[F]
  val ads: AdvertisementRepository[F]
  val tags: TagRepository[F]
  val chats: ChatRepository[F]
  val messages: MessageRepository[F]
  val images: AdImageRepository[F]
  val feed: FeedRepository[F]
}

object Repositories {
  def makeCassandra[F[_]: Async](cassandra: CassandraSession[F]): Repositories[F] = {
    new Repositories[F] {
      override val users: UserRepository[F]       = CassandraUserRepository.make[F](cassandra)
      override val messages: MessageRepository[F] = CassandraMessageRepository.make[F](cassandra)
      override val chats: ChatRepository[F]       = CassandraChatRepository.make[F](cassandra)
      override val ads: AdvertisementRepository[F] =
        CassandraAdvertisementRepository.make[F](cassandra)
      override val images: AdImageRepository[F] = CassandraAdImageRepository.make[F](cassandra)
      override val tags: TagRepository[F]       = CassandraTagRepository.make[F](cassandra)
      override val feed: FeedRepository[F]      = CassandraFeedRepository.make[F](cassandra)
    }
  }
}
