package com.holodome.domain.services

import com.holodome.domain.ads.*
import com.holodome.domain.messages.{Chat, ChatId}
import com.holodome.domain.users.UserId

import cats.data.OptionT

trait ChatService[F[_]]:
  def get(id: ChatId, requester: UserId): F[Chat]
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, Chat]
