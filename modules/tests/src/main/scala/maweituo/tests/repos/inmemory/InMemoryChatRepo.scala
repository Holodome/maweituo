package maweituo
package tests
package repos
package inmemory

import scala.collection.concurrent.TrieMap

import maweituo.domain.all.*

class InMemoryChatRepo[F[_]: Sync] extends ChatRepo[F]:

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

  override def findForAd(ad: AdId): F[List[Chat]] =
    Sync[F] delay map.values.filter(chat => chat.adId === ad).toList

  override def findForUser(userId: UserId): F[List[Chat]] =
    Sync[F] delay map.values.filter(chat => chat.adAuthor === userId || chat.client === userId).toList
