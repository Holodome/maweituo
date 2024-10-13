package maweituo
package tests
package repos
package inmemory

object InMemoryRepoFactory:
  def images[F[_]: Sync]: AdImageRepo[F] = new InMemoryAdImageRepo[F]
  def ads[F[_]: Sync]: AdRepo[F]         = new InMemoryAdRepo[F]
  def chats[F[_]: Sync]: ChatRepo[F]     = new InMemoryChatRepo[F]
  def tags[F[_]: Sync]: AdTagRepo[F]     = new InMemoryAdTagRepo[F]
  def users[F[_]: Sync]: UserRepo[F]     = new InMemoryUserRepo[F]
