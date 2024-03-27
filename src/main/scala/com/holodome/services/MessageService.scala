package com.holodome.services

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId
import com.holodome.repositories.MessageRepository

trait MessageService[F[_]] {
  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
}

object MessageService {
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      iam: IAMService[F]
  ): MessageService[F] =
    new MessageServiceInterpreter(msgRepo, iam)

  private final class MessageServiceInterpreter[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      iam: IAMService[F]
  ) extends MessageService[F] {

    override def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit] =
      iam.authorizeChatAccess(chatId, senderId) >> msgRepo.send(chatId, senderId, req.text)

    override def history(chatId: ChatId, requester: UserId): F[HistoryResponse] =
      iam.authorizeChatAccess(chatId, requester) >> msgRepo
        .chatHistory(chatId)
        .map(HistoryResponse.apply)

  }
}
