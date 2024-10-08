package maweituo.domain.ads.repos

import maweituo.domain.ads.messages.*

trait MessageRepo[F[_]]:
  def chatHistory(chatId: ChatId): F[List[Message]]
  def send(message: Message): F[Unit]
