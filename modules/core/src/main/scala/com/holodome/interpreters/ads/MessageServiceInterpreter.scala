package com.holodome.interpreters.ads

import com.holodome.domain.ads.repos.MessageRepository
import com.holodome.domain.ads.services.MessageService
import com.holodome.domain.messages.*
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId
import com.holodome.effects.TimeSource

import cats.MonadThrow
import cats.syntax.all.*

object MessageServiceInterpreter:
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepository[F]
  )(using clock: TimeSource[F], iam: IAMService[F]): MessageService[F] = new:
    def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit] =
      for
        _   <- iam.authChatAccess(chatId, senderId)
        now <- clock.instant
        msg = Message(
          senderId,
          chatId,
          req.text,
          now
        )
        _ <- msgRepo.send(msg)
      yield ()

    def history(chatId: ChatId, requester: UserId): F[HistoryResponse] =
      iam.authChatAccess(chatId, requester) *> msgRepo
        .chatHistory(chatId)
        .map(HistoryResponse.apply)
