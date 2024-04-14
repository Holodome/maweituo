package com.holodome.repositories

import cats.data.OptionT
import cats.MonadThrow
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.InvalidChatId
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait ChatRepository[F[_]] {
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, ChatId]
}

object ChatRepository {
  implicit class ChatRepositoryOps[F[_]: MonadThrow](repo: ChatRepository[F]) {
    def get(chatId: ChatId): F[Chat] =
      repo.find(chatId).getOrRaise(InvalidChatId())
  }
}
