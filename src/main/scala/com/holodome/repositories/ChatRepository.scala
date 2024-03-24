package com.holodome.repositories

import cats.data.OptionT
import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId

trait ChatRepository[F[_]] {
  def create(adId: AdvertisementId, adAuthor: UserId, client: UserId): F[ChatId]
  def getByAdAndClient(adId: AdvertisementId, client: UserId): OptionT[F, ChatId]
}
