package com.holodome.services

import com.holodome.domain.advertisements.AdvertisementId
import com.holodome.domain.chats._
import com.holodome.domain.users.UserId

trait ChatService[F[_]] {
  def find(id: ChatId): F[Chat]
  def create(adId: AdvertisementId, authorId: UserId, clientId: UserId): F[Unit]
  def findByAdvertisement(adId: AdvertisementId): F[List[Chat]]
  def findByClient(uid: UserId): F[List[Chat]]
  def findByAuthor(uid: UserId): F[List[Chat]]
}
