package com.holodome.domain.services

import com.holodome.domain.ads.*
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.*
import com.holodome.domain.users.UserId

trait IAMService[F[_]]:
  def authChatAccess(chatId: ChatId, userId: UserId): F[Unit]
  def authAdModification(advertisementId: AdId, userId: UserId): F[Unit]
  def authUserModification(target: UserId, userId: UserId): F[Unit]
  def authImageDelete(imageId: ImageId, userId: UserId): F[Unit]
