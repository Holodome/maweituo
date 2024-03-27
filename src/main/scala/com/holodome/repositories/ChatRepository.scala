package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements.AdId
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait ChatRepository[F[_]] {
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, ChatId]
}
