package maweituo
package domain
package repos
package ads

import maweituo.domain.ads.*
import maweituo.domain.messages.*
import maweituo.domain.users.UserId

trait ChatRepo[F[_]]:
  def create(chat: Chat): F[Unit]
  def find(chatId: ChatId): OptionT[F, Chat]
  def findByAdAndClient(adId: AdId, client: UserId): OptionT[F, Chat]

object ChatRepo:
  import maweituo.logic.errors.DomainError

  extension [F[_]: MonadThrow](repo: ChatRepo[F])
    def get(chatId: ChatId): F[Chat] =
      repo.find(chatId).getOrRaise(DomainError.InvalidChatId(chatId))
