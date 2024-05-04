package com.holodome.services

import cats.data.OptionT
import com.holodome.domain.ads._
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId

trait ChatService[F[_]] {
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, ChatId]
}
