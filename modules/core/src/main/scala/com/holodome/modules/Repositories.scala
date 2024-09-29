package com.holodome.modules

import com.holodome.domain.repositories.*
import com.holodome.postgres.repositories.PostgresAdImageRepository
import com.holodome.postgres.repositories.PostgresAdRepository
import com.holodome.postgres.repositories.PostgresChatRepository
import com.holodome.postgres.repositories.PostgresFeedRepository
import com.holodome.postgres.repositories.PostgresMessageRepository
import com.holodome.postgres.repositories.PostgresTagRepository
import com.holodome.postgres.repositories.PostgresUserRepository

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

sealed abstract class Repositories[F[_]]:
  val users: UserRepository[F]
  val ads: AdvertisementRepository[F]
  val tags: TagRepository[F]
  val chats: ChatRepository[F]
  val messages: MessageRepository[F]
  val images: AdImageRepository[F]
  val feed: FeedRepository[F]

object Repositories:
  def makePostgres[F[_]: Async](xa: Transactor[F]): Repositories[F] = new:
    val users    = PostgresUserRepository.make[F](xa)
    val ads      = PostgresAdRepository.make[F](xa)
    val tags     = PostgresTagRepository.make[F](xa)
    val chats    = PostgresChatRepository.make[F](xa)
    val messages = PostgresMessageRepository.make[F](xa)
    val images   = PostgresAdImageRepository.make[F](xa)
    val feed     = PostgresFeedRepository.make[F](xa)
