package maweituo.interp.ads

import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.messages.*
import maweituo.domain.ads.repos.MessageRepo
import maweituo.domain.ads.services.MessageService
import maweituo.domain.services.IAMService
import maweituo.effects.TimeSource

object MessageServiceInterp:
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepo[F]
  )(using clock: TimeSource[F], iam: IAMService[F]): MessageService[F] = new:
    def send(chatId: ChatId, req: SendMessageRequest)(using Identity): F[Unit] =
      for
        _   <- iam.authChatAccess(chatId)
        now <- clock.instant
        msg = Message(
          summon[Identity].id,
          chatId,
          req.text,
          now
        )
        _ <- msgRepo.send(msg)
      yield ()

    def history(chatId: ChatId)(using Identity): F[HistoryResponse] =
      iam.authChatAccess(chatId) *> msgRepo
        .chatHistory(chatId)
        .map(HistoryResponse.apply)
