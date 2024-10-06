package maweituo.domain.ads.services

import cats.data.OptionT

import maweituo.domain.Identity
import maweituo.domain.ads.*
import maweituo.domain.ads.messages.{Chat, ChatId}

trait ChatService[F[_]]:
  def get(id: ChatId)(using Identity): F[Chat]
  def create(adId: AdId)(using Identity): F[ChatId]
  def findForAdAndUser(ad: AdId)(using Identity): OptionT[F, Chat]
