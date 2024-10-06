package maweituo.domain.services

import maweituo.domain.Identity
import maweituo.domain.ads.*
import maweituo.domain.ads.images.ImageId
import maweituo.domain.ads.messages.*
import maweituo.domain.users.UserId

trait IAMService[F[_]]:
  def authChatAccess(chatId: ChatId)(using Identity): F[Unit]
  def authAdModification(advertisementId: AdId)(using Identity): F[Unit]
  def authUserModification(target: UserId)(using Identity): F[Unit]
  def authImageDelete(imageId: ImageId)(using Identity): F[Unit]
