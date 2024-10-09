package maweituo.domain.ads.repos

import cats.MonadThrow
import cats.data.OptionT

import maweituo.domain.ads.AdId
import maweituo.domain.ads.messages.*
import maweituo.domain.users.UserId
import maweituo.logic.errors.DomainError

trait ChatRepo[F[_]]:
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat]

object ChatRepo:
  extension [F[_]: MonadThrow](repo: ChatRepo[F])
    def get(chatId: ChatId): F[Chat] =
      repo.find(chatId).getOrRaise(DomainError.InvalidChatId(chatId))
