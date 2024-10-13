package maweituo
package domain
package services
package ads
import maweituo.domain.messages.*

trait MessageService[F[_]]:
  def send(chatId: ChatId, req: SendMessageRequest)(using Identity): F[Unit]
  def history(chatId: ChatId, pag: Pagination)(using Identity): F[PaginatedCollection[Message]]
