package maweituo
package postgres
package repos
package ads

import cats.effect.Async
import cats.syntax.all.*
import cats.Parallel

import maweituo.domain.all.*

import doobie.Transactor
import doobie.implicits.given
import doobie.postgres.implicits.given

object PostgresMessageRepo:
  def make[F[_]: Async: Parallel](xa: Transactor[F]): MessageRepo[F] = new:

    def chatHistory(chatId: ChatId, pag: Pagination): F[PaginatedCollection[Message]] =
      val count = sql"select count(*) from messages where chat_id = $chatId::uuid".query[Int].unique.transact(xa)
      val msgs = sql"""select sender_id, chat_id, msg, at from messages 
            where chat_id = $chatId::uuid
            order by at desc 
            limit ${pag.limit} offset ${pag.offset}
      """.query[Message].to[List].transact(xa)
      (count, msgs).parMapN { (count, items) => PaginatedCollection(items, pag, count) }

    def send(message: Message): F[Unit] =
      sql"""
      insert into messages(sender_id, chat_id, msg, at) 
      values (${message.sender}::uuid, ${message.chat}::uuid, ${message.text}, ${message.at})
      """.update.run.transact(xa).void
