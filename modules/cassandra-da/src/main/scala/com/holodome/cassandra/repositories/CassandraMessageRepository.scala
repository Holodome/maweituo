package com.holodome.cassandra.repositories

import cats.effect.Async
import cats.syntax.all._
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.holodome.cassandra.cql.codecs._
import com.holodome.domain.messages.{ChatId, Message}
import com.holodome.domain.repositories.MessageRepository
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql.CqlStringContext

object CassandraMessageRepository {
  def make[F[_]: Async](session: CassandraSession[F]): MessageRepository[F] =
    new CassandraMessageRepository(session)
}

private final class CassandraMessageRepository[F[_]: Async](session: CassandraSession[F])
    extends MessageRepository[F] {

  override def chatHistory(chatId: ChatId): F[List[Message]] =
    cql"select sender_id, chat_id, msg, at from local.messages where chat_id = ${chatId.value}"
      .as[Message]
      .select(session)
      .compile
      .toList

  override def send(message: Message): F[Unit] =
    cql"insert into local.messages (sender_id, chat_id, msg, at) values (${message.sender.value}, ${message.chat.value}, ${message.text.value}, ${message.at})"
      .config(
        _.setConsistencyLevel(ConsistencyLevel.ONE)
      )
      .execute(session)
      .void

}
