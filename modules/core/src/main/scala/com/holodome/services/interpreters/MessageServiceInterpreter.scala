package com.holodome.services.interpreters

import cats.MonadThrow
import cats.syntax.all._
import com.holodome.domain.messages._
import com.holodome.domain.repositories.MessageRepository
import com.holodome.domain.services.{IAMService, MessageService}
import com.holodome.domain.users.UserId
import com.holodome.effects.TimeSource

object MessageServiceInterpreter {
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      iam: IAMService[F]
  )(implicit clock: TimeSource[F]): MessageService[F] =
    new MessageServiceInterpreter(msgRepo, iam)

}

private final class MessageServiceInterpreter[F[_]: MonadThrow](
    msgRepo: MessageRepository[F],
    iam: IAMService[F]
)(implicit timeSource: TimeSource[F])
    extends MessageService[F] {

  override def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit] = for {
    _   <- iam.authorizeChatAccess(chatId, senderId)
    now <- timeSource.instant
    msg = Message(
      senderId,
      chatId,
      req.text,
      now
    )
    _ <- msgRepo.send(msg)
  } yield ()

  override def history(chatId: ChatId, requester: UserId): F[HistoryResponse] =
    iam.authorizeChatAccess(chatId, requester) *> msgRepo
      .chatHistory(chatId)
      .map(HistoryResponse.apply)

}
