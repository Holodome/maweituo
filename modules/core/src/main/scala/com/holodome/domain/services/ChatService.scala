package com.holodome.domain.services

import cats.data.OptionT
import com.holodome.domain.ads._
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.domain.messages.Chat

trait ChatService[F[_]] {
  def get(id: ChatId, requester: UserId): F[Chat]
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, ChatId]
}
