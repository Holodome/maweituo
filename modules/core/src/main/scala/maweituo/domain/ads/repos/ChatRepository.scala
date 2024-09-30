package maweituo.domain.ads.repos

import maweituo.domain.ads.AdId
import maweituo.domain.errors.InvalidChatId
import maweituo.domain.messages.*
import maweituo.domain.users.UserId

import cats.MonadThrow
import cats.data.OptionT

trait ChatRepository[F[_]]:
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat]

object ChatRepository:
  extension [F[_]: MonadThrow](repo: ChatRepository[F])
    def get(chatId: ChatId): F[Chat] =
      repo.find(chatId).getOrRaise(InvalidChatId(chatId))
