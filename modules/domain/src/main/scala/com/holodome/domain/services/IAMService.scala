package com.holodome.domain.services

import com.holodome.domain.ads._
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId

trait IAMService[F[_]] {
  def authorizeChatAccess(chatId: ChatId, userId: UserId): F[Unit]
  def authorizeAdModification(advertisementId: AdId, userId: UserId): F[Unit]
  def authorizeUserModification(target: UserId, userId: UserId): F[Unit]
  def authorizeImageDelete(imageId: ImageId, userId: UserId): F[Unit]
}