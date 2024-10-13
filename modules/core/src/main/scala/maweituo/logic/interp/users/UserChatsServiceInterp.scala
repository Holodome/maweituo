package maweituo
package logic
package interp
package users
import cats.MonadThrow
import cats.syntax.all.*

import maweituo.domain.all.*

object UserChatsServiceInterp:
  def make[F[_]: MonadThrow](chats: ChatRepo[F])(using iam: IAMService[F]): UserChatsService[F] = new:
    def getChats(userId: UserId)(using Identity): F[List[Chat]] =
      chats.findForUser(userId).flatTap { chats =>
        chats.map(_.id).traverse_(iam.authChatAccess)
      }
