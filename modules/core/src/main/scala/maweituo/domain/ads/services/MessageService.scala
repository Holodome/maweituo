package maweituo.domain.ads.services

import maweituo.domain.messages.*
import maweituo.domain.users.UserId

trait MessageService[F[_]]:
  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
