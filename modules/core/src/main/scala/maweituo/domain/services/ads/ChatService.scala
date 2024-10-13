package maweituo
package domain
package services
package ads

import cats.data.OptionT

import maweituo.domain.ads.*
import maweituo.domain.messages.{Chat, ChatId}

trait ChatService[F[_]]:
  def get(id: ChatId)(using Identity): F[Chat]
  def create(adId: AdId)(using Identity): F[ChatId]
  def findForAdAndUser(ad: AdId)(using Identity): OptionT[F, Chat]
  def findForAd(ad: AdId)(using Identity): F[List[Chat]]
