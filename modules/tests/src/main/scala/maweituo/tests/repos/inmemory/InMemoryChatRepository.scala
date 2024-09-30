package maweituo.tests.repos.inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.ads.AdId
import maweituo.domain.ads.messages.{ Chat, ChatId }
import maweituo.domain.ads.repos.ChatRepository
import maweituo.domain.users.UserId
import maweituo.domain.{ ads, users }

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

private final class InMemoryChatRepository[F[_]: Sync] extends ChatRepository[F]:
  private val map = new TrieMap[ChatId, Chat]

  override def create(chat: Chat): F[Unit] =
    Sync[F] delay map.addOne(chat.id -> chat)

  override def find(chatId: ChatId): OptionT[F, Chat] =
    OptionT(Sync[F] delay map.get(chatId))

  override def findByAdAndClient(
      adId: AdId,
      client: UserId
  ): OptionT[F, Chat] =
    OptionT(Sync[F] delay map.values.find(chat => chat.adId === adId && chat.client === client))
