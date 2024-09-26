package com.holodome.interpreters

import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.{ ChatAccessForbidden, InvalidAccess, NotAnAuthor }
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.{ Chat, ChatId }
import com.holodome.domain.repositories.{ AdImageRepository, AdvertisementRepository, ChatRepository }
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId

import cats.syntax.all.*
import cats.{ Applicative, MonadThrow }

object IAMServiceInterpreter:
  def make[F[_]: MonadThrow](
      adRepo: AdvertisementRepository[F],
      chatRepo: ChatRepository[F],
      imageRepo: AdImageRepository[F]
  ): IAMService[F] = new:
    def authChatAccess(chatId: ChatId, userId: UserId): F[Unit] =
      chatRepo
        .get(chatId)
        .flatMap {
          case chat if userHasAccessToChat(chat, userId) =>
            Applicative[F].unit
          case _ => ChatAccessForbidden(chatId).raiseError[F, Unit]
        }

    def authAdModification(adId: AdId, userId: UserId): F[Unit] =
      adRepo
        .get(adId)
        .flatMap {
          case ad if ad.authorId === userId => Applicative[F].unit
          case _                            => NotAnAuthor(adId, userId).raiseError[F, Unit]
        }

    def authUserModification(target: UserId, userId: UserId): F[Unit] =
      (target === userId)
        .guard[Option]
        .fold(InvalidAccess("User update not authd").raiseError[F, Unit])(Applicative[F].pure)

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor || user === chat.client

    def authImageDelete(imageId: ImageId, userId: UserId): F[Unit] =
      imageRepo
        .get(imageId)
        .flatMap(image => authAdModification(image.adId, userId))
