package maweituo.interp.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.ads.messages.*
import maweituo.domain.ads.repos.MessageRepo
import maweituo.domain.ads.services.MessageService
import maweituo.domain.services.IAMService
import maweituo.domain.users.UserId
import maweituo.effects.TimeSource

object MessageServiceInterp:
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepo[F]
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
