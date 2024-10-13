package maweituo
package domain
package services
package users

import maweituo.domain.messages.Chat
import maweituo.domain.users.UserId

trait UserChatsService[F[_]]:
  def getChats(userId: UserId)(using Identity): F[List[Chat]]
