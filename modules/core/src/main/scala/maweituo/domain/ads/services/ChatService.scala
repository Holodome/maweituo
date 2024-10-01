package maweituo.domain.ads.services

import cats.data.OptionT

import maweituo.domain.ads.*
import maweituo.domain.ads.messages.{Chat, ChatId}
import maweituo.domain.users.UserId

trait ChatService[F[_]]:
  def get(id: ChatId, requester: UserId): F[Chat]
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, Chat]
