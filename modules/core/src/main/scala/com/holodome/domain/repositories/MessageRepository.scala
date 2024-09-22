package com.holodome.domain.repositories

import com.holodome.domain.messages.*

trait MessageRepository[F[_]]:
  def chatHistory(chatId: ChatId): F[List[Message]]
  def send(message: Message): F[Unit]
