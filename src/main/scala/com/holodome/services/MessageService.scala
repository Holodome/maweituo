package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.instances.unit
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId
import com.holodome.repositories.{ChatRepository, MessageRepository}

trait MessageService[F[_]] {

  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
}

object MessageService {
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      chatService: ChatService[F],
  ): MessageService[F] =
    new MessageServiceInterpreter(msgRepo, chatService)

  private final class MessageServiceInterpreter[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      chatService: ChatService[F],
  ) extends MessageService[F] {

    override def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit] =
      chatService.findChatAndCheckAccess(chatId, senderId).flatMap { _ =>
        msgRepo.send(chatId, senderId, req.text)
      }

    override def history(chatId: ChatId, requester: UserId): F[HistoryResponse] =
      chatService
        .findChatAndCheckAccess(chatId, requester)
        .flatMap(_ => msgRepo.chatHistory(chatId).map(HistoryResponse.apply))

  }
}
