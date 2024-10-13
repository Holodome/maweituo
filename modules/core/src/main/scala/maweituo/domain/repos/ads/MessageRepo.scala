package maweituo
package domain
package repos
package ads

import maweituo.domain.messages.{ChatId, Message}

trait MessageRepo[F[_]]:
  def chatHistory(chatId: ChatId, pag: Pagination): F[PaginatedCollection[Message]]
  def send(message: Message): F[Unit]
