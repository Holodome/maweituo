package com.holodome.tests.services.stubs

import com.holodome.domain.ads.AdId
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.ChatId
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId

import cats.Monad

final class IAMServiceStub[F[_]: Monad] extends IAMService[F]:

  override def authChatAccess(chatId: ChatId, userId: UserId): F[Unit] = Monad[F].unit

  override def authAdModification(advertisementId: AdId, userId: UserId): F[Unit] = Monad[F].unit

  override def authUserModification(target: UserId, userId: UserId): F[Unit] = Monad[F].unit

  override def authImageDelete(imageId: ImageId, userId: UserId): F[Unit] = Monad[F].unit
