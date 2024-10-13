package maweituo
package tests
package repos
package inmemory

import scala.collection.concurrent.TrieMap

class InMemoryMessageRepo[F[_]: Sync] extends MessageRepo[F]:

  private val map = new TrieMap[Message, Unit]

  override def chatHistory(chatId: ChatId): F[List[Message]] =
    Sync[F] delay map.keys.filter(_.chat === chatId).toList.sortBy(_.at)

  override def send(message: Message): F[Unit] =
    Sync[F] delay map.addOne(message -> ())
