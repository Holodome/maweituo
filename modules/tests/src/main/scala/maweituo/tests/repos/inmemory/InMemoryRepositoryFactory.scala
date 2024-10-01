package maweituo.tests.repos.inmemory
import cats.effect.kernel.Sync

import maweituo.domain.ads.repos.{AdImageRepo, AdRepo, AdTagRepo, ChatRepo, MessageRepo}
import maweituo.domain.users.repos.UserRepo

object InMemoryRepoFactory:
  def images[F[_]: Sync]: AdImageRepo[F] = new InMemoryAdImageRepo[F]
  def ads[F[_]: Sync]: AdRepo[F]         = new InMemoryAdRepo[F]
  def chats[F[_]: Sync]: ChatRepo[F]     = new InMemoryChatRepo[F]
  def msgs[F[_]: Sync]: MessageRepo[F]   = new InMemoryMessageRepo[F]
  def tags[F[_]: Sync]: AdTagRepo[F]     = new InMemoryAdTagRepo[F]
  def users[F[_]: Sync]: UserRepo[F]     = new InMemoryUserRepo[F]
