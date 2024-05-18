package com.holodome.domain.services

import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait MessageService[F[_]] {
  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
}
