package com.holodome.domain.repositories

import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.InvalidChatId
import com.holodome.domain.messages.*
import com.holodome.domain.users.UserId

import cats.MonadThrow
import cats.data.OptionT

trait ChatRepository[F[_]]:
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, ChatId]

object ChatRepository:
  extension [F[_]: MonadThrow](repo: ChatRepository[F])
    def get(chatId: ChatId): F[Chat] =
      repo.find(chatId).getOrRaise(InvalidChatId(chatId))
