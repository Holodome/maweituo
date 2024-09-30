package maweituo.tests.repos.inmemory
import maweituo.domain.ads.repos.{ AdImageRepository, AdRepository, AdTagRepository, ChatRepository, MessageRepository }
import maweituo.domain.users.repos.UserRepository

import cats.effect.kernel.Sync

object InMemoryRepositoryFactory:
  def images[F[_]: Sync]: AdImageRepository[F] = new InMemoryAdImageRepository[F]
  def ads[F[_]: Sync]: AdRepository[F]         = new InMemoryAdRepository[F]
  def chats[F[_]: Sync]: ChatRepository[F]     = new InMemoryChatRepository[F]
  def msgs[F[_]: Sync]: MessageRepository[F]   = new InMemoryMessageRepository[F]
  def tags[F[_]: Sync]: AdTagRepository[F]     = new InMemoryAdTagRepository[F]
  def users[F[_]: Sync]: UserRepository[F]     = new InMemoryUserRepository[F]
