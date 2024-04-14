package com.holodome.repositories.cassandra

import cats.syntax.all._
import cats.effect.Async
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.domain.messages.{ChatId, Message}
import com.holodome.repositories.MessageRepository
import com.ringcentral.cassandra4io.cql.CqlStringContext
import com.ringcentral.cassandra4io.CassandraSession
import com.holodome.cql.codecs._

object CassandraMessageRepository {
  def make[F[_]: Async](session: CassandraSession[F]): MessageRepository[F] =
    new CassandraMessageRepository(session)
}

private final class CassandraMessageRepository[F[_]: Async](session: CassandraSession[F])
    extends MessageRepository[F] {

  override def chatHistory(chatId: ChatId): F[List[Message]] =
    chatHistoryQuery(chatId).select(session).compile.toList

  override def send(message: Message): F[Unit] =
    sendQuery(message).execute(session).void

  private def chatHistoryQuery(chatId: ChatId) =
    cql"select sender_id, chat_id, msg, at from local.messages where chat_id = ${chatId.id}"
      .as[Message]

  private def sendQuery(message: Message) =
    cql"insert into local.messages (sender_id, chat_id, msg, at) values (${message.sender.value}, ${message.chat.id}, ${message.text.value}, ${message.at})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.ONE)
      )
}
