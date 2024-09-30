package com.holodome.tests.repos.inmemory
import com.holodome.domain.ads.repos.*
import com.holodome.domain.users.repos.UserRepository

import cats.effect.kernel.Sync

object InMemoryRepositoryFactory:
  def images[F[_]: Sync]: AdImageRepository[F] = new InMemoryAdImageRepository[F]
  def ads[F[_]: Sync]: AdRepository[F]         = new InMemoryAdRepository[F]
  def chats[F[_]: Sync]: ChatRepository[F]     = new InMemoryChatRepository[F]
  def msgs[F[_]: Sync]: MessageRepository[F]   = new InMemoryMessageRepository[F]
  def tags[F[_]: Sync]: AdTagRepository[F]     = new InMemoryAdTagRepository[F]
  def users[F[_]: Sync]: UserRepository[F]     = new InMemoryUserRepository[F]
