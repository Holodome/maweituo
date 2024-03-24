package com.holodome.services

import cats.MonadThrow
import com.holodome.domain.chats.ChatId
import com.holodome.domain.messages.Message
import com.holodome.repositories.MessageRepository

trait MessageService[F[_]] {
  def send(message: Message): F[Unit]
  def history(chat: ChatId): F[List[Message]]
}

object MessageService {
  def make[F[_]: MonadThrow](repo: MessageRepository[F]): MessageService[F] =
    new MessageServiceInterpreter(repo)

  private final class MessageServiceInterpreter[F[_]: MonadThrow](repo: MessageRepository[F])
      extends MessageService[F] {

    override def send(message: Message): F[Unit] = ???

    override def history(chat: ChatId): F[List[Message]] = ???
  }
}
