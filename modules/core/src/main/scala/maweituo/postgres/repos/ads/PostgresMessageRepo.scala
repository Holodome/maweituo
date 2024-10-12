package maweituo
package postgres
package repos
package ads

import cats.effect.Async
import cats.syntax.all.*

import maweituo.domain.all.*

import doobie.Transactor
import doobie.implicits.given
import doobie.postgres.implicits.given

object PostgresMessageRepo:
  def make[F[_]: Async](xa: Transactor[F]): MessageRepo[F] = new:

    def chatHistory(chatId: ChatId): F[List[Message]] =
      sql"select sender_id, chat_id, msg, at from messages where chat_id = $chatId::uuid"
        .query[Message].to[List].transact(xa)

    def send(message: Message): F[Unit] =
      sql"""
      insert into messages(sender_id, chat_id, msg, at) 
      values (${message.sender}::uuid, ${message.chat}::uuid, ${message.text}, ${message.at})
      """.update.run.transact(xa).void
