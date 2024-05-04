package com.holodome.tests.repositories

import cats.effect.Sync
import com.holodome.domain.messages
import com.holodome.domain.messages.Message
import com.holodome.domain.repositories.MessageRepository

import scala.collection.concurrent.TrieMap

class InMemoryMessageRepository[F[_]: Sync] extends MessageRepository[F] {

  private val map = new TrieMap[Message, Unit]

  override def chatHistory(chatId: messages.ChatId): F[List[messages.Message]] =
    Sync[F].delay { map.keys.toList }

  override def send(message: Message): F[Unit] = Sync[F].delay { map.addOne(message -> ()) }
}
