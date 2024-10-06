package maweituo.domain.ads.services

import maweituo.domain.Identity
import maweituo.domain.ads.messages.*

trait MessageService[F[_]]:
  def send(chatId: ChatId, req: SendMessageRequest)(using Identity): F[Unit]
  def history(chatId: ChatId)(using Identity): F[HistoryResponse]
