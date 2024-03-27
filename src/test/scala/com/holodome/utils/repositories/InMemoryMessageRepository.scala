package com.holodome.utils.repositories

import cats.effect.kernel.Sync
import com.holodome.domain.messages
import com.holodome.domain.messages.{Message, MessageId}
import com.holodome.repositories.MessageRepository

import scala.collection.concurrent.TrieMap

class InMemoryMessageRepository[F[_]: Sync] extends MessageRepository[F] {

  private val map = new TrieMap[MessageId, Message]

  override def chatHistory(chatId: messages.ChatId): F[List[messages.Message]] =
    Sync[F].delay { map.values.toList }

  override def send(message: Message): F[Unit] = Sync[F].delay { map.addOne(message.id -> message) }
}
