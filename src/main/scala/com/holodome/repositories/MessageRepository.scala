package com.holodome.repositories

import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait MessageRepository[F[_]] {
  def chatHistory(chatId: ChatId): F[List[Message]]
  def send(message: Message): F[Unit]
}
