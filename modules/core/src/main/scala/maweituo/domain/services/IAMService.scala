package maweituo.domain.services

import maweituo.domain.ads.*
import maweituo.domain.images.ImageId
import maweituo.domain.messages.*
import maweituo.domain.users.UserId

trait IAMService[F[_]]:
  def authChatAccess(chatId: ChatId, userId: UserId): F[Unit]
  def authAdModification(advertisementId: AdId, userId: UserId): F[Unit]
  def authUserModification(target: UserId, userId: UserId): F[Unit]
  def authImageDelete(imageId: ImageId, userId: UserId): F[Unit]
