package maweituo
package domain
package repos
package ads

import maweituo.domain.messages.{ChatId, Message}

trait MessageRepo[F[_]]:
  def chatHistory(chatId: ChatId): F[List[Message]]
  def send(message: Message): F[Unit]
