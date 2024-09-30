package com.holodome.domain.ads.services

import com.holodome.domain.messages.*
import com.holodome.domain.users.UserId

trait MessageService[F[_]]:
  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
