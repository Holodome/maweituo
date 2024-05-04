package com.holodome.repositories

import com.holodome.domain.messages._

trait MessageRepository[F[_]] {
  def chatHistory(chatId: ChatId): F[List[Message]]
  def send(message: Message): F[Unit]
}
