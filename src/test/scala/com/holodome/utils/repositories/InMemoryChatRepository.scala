package com.holodome.utils.repositories

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.domain.{advertisements, users}
import com.holodome.domain.messages._
import com.holodome.repositories.ChatRepository

import scala.collection.concurrent.TrieMap

final class InMemoryChatRepository[F[_]: Sync] extends ChatRepository[F] {
  private val map = new TrieMap[ChatId, Chat]

  override def create(chat: Chat): F[Unit] =
    Sync[F].delay { map.addOne(chat.id -> chat) }

  override def find(chatId: ChatId): OptionT[F, Chat] =
    OptionT(Sync[F].delay { map.get(chatId) })

  override def findByAdAndClient(
      adId: advertisements.AdId,
      client: users.UserId
  ): OptionT[F, ChatId] =
    OptionT(Sync[F].delay {
      map.values.find(chat => chat.adId === adId && chat.client === client).map(_.id)
    })
}
