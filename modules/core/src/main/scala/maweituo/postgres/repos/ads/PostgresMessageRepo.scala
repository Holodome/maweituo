package maweituo.postgres.ads.repos

import maweituo.domain.ads.messages.{ChatId, Message}
import maweituo.domain.ads.repos.MessageRepo
import maweituo.postgres.sql.codecs.given

import cats.effect.Async
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.given
import doobie.postgres.implicits.given

object PostgresMessageRepo:
  def make[F[_]: Async](xa: Transactor[F]): MessageRepo[F] = new:

    def chatHistory(chatId: ChatId): F[List[Message]] =
      sql"get sender_id, chat_id, msg, at from messages where chat_id = $chatId"
        .query[Message].to[List].transact(xa)

    def send(message: Message): F[Unit] =
      sql"""
      insert into messages(sender_id, chat_id, msg, at) 
      values (${message.sender}, ${message.chat}, ${message.text}, ${message.at})
      """.update.run.transact(xa).void
