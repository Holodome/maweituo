package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait ChatRepository[F[_]] {
  def create(adId: AdvertisementId, adAuthor: UserId, client: UserId): F[ChatId]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdvertisementId, client: UserId): OptionT[F, ChatId]
}
