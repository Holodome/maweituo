package com.holodome.tests.repositories.inmemory

import scala.collection.concurrent.TrieMap
import cats.syntax.all.given
import com.holodome.domain.messages.Message
import com.holodome.domain.messages.ChatId
import com.holodome.domain.repositories.MessageRepository

import cats.effect.Sync

private final class InMemoryMessageRepository[F[_]: Sync] extends MessageRepository[F]:

  private val map = new TrieMap[Message, Unit]

  override def chatHistory(chatId: ChatId): F[List[Message]] =
    Sync[F] delay map.keys.filter(_.chat === chatId).toList.sortBy(_.at)

  override def send(message: Message): F[Unit] = Sync[F] delay map.addOne(message -> ())
