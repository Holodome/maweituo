package com.holodome.modules

import com.holodome.domain.ads.repos.*
import com.holodome.domain.repos.*
import com.holodome.domain.users.repos.UserRepository
import com.holodome.postgres.ads.repos.*
import com.holodome.postgres.repos.*
import com.holodome.postgres.repos.users.*

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

sealed abstract class Repositories[F[_]]:
  val users: UserRepository[F]
  val ads: AdRepository[F]
  val tags: AdTagRepository[F]
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
