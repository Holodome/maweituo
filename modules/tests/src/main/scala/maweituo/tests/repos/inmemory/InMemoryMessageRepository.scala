package maweituo.tests.repos.inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.ads.messages.{ChatId, Message}
import maweituo.domain.ads.repos.MessageRepository

import cats.effect.Sync
import cats.syntax.all.given

class InMemoryMessageRepository[F[_]: Sync] extends MessageRepository[F]:

  private val map = new TrieMap[Message, Unit]

  override def chatHistory(chatId: ChatId): F[List[Message]] =
    Sync[F] delay map.keys.filter(_.chat === chatId).toList.sortBy(_.at)

  override def send(message: Message): F[Unit] =
    Sync[F] delay map.addOne(message -> ())
