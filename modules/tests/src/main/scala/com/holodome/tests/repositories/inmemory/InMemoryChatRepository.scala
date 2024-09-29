package com.holodome.tests.repositories.inmemory

import scala.collection.concurrent.TrieMap

import com.holodome.domain.ads.AdId
import com.holodome.domain.messages.*
import com.holodome.domain.repositories.ChatRepository
import com.holodome.domain.users.UserId
import com.holodome.domain.{ ads, users }

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

final class InMemoryChatRepository[F[_]: Sync] extends ChatRepository[F]:
  private val map = new TrieMap[ChatId, Chat]

  override def create(chat: Chat): F[Unit] =
    Sync[F] delay map.addOne(chat.id -> chat)

  override def find(chatId: ChatId): OptionT[F, Chat] =
    OptionT(Sync[F] delay map.get(chatId))

  override def findByAdAndClient(
      adId: AdId,
      client: UserId
  ): OptionT[F, Chat] =
    OptionT(Sync[F].delay {
      map.values.find(chat => chat.adId === adId && chat.client === client)
    })
