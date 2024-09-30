package maweituo.domain.ads.services

import maweituo.domain.ads.*
import maweituo.domain.ads.messages.{ Chat, ChatId }
import maweituo.domain.users.UserId

import cats.data.OptionT

trait ChatService[F[_]]:
  def get(id: ChatId, requester: UserId): F[Chat]
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, Chat]
