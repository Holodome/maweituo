package com.holodome.interpreters

import cats.syntax.all._
import cats.{Applicative, MonadThrow}
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.{ChatAccessForbidden, InvalidAccess, NotAnAuthor}
import com.holodome.domain.images.ImageId
import com.holodome.domain.messages.{Chat, ChatId}
import com.holodome.domain.repositories.{AdImageRepository, AdvertisementRepository, ChatRepository}
import com.holodome.domain.services.IAMService
import com.holodome.domain.users.UserId

object IAMServiceInterpreter {

  def make[F[_]: MonadThrow](
      adRepo: AdvertisementRepository[F],
      chatRepo: ChatRepository[F],
      imageRepo: AdImageRepository[F]
  ): IAMService[F] =
    new IAMServiceInterpreter(adRepo, chatRepo, imageRepo)

}

private final class IAMServiceInterpreter[F[_]: MonadThrow](
    adRepo: AdvertisementRepository[F],
    chatRepo: ChatRepository[F],
    imageRepo: AdImageRepository[F]
) extends IAMService[F] {

  override def authorizeChatAccess(chatId: ChatId, userId: UserId): F[Unit] =
    chatRepo
      .get(chatId)
      .flatMap {
        case chat if userHasAccessToChat(chat, userId) =>
          Applicative[F].unit
        case _ => ChatAccessForbidden(chatId).raiseError[F, Unit]
      }

  override def authorizeAdModification(adId: AdId, userId: UserId): F[Unit] =
    adRepo
      .get(adId)
      .flatMap {
        case ad if ad.authorId === userId => Applicative[F].unit
        case _                            => NotAnAuthor(adId, userId).raiseError[F, Unit]
      }

  override def authorizeUserModification(target: UserId, userId: UserId): F[Unit] =
    (target === userId)
      .guard[Option]
      .fold(InvalidAccess("User update not authorized").raiseError[F, Unit])(Applicative[F].pure)

  private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
    user === chat.adAuthor || user === chat.client

  override def authorizeImageDelete(imageId: ImageId, userId: UserId): F[Unit] =
    imageRepo
      .getMeta(imageId)
      .flatMap(image => authorizeAdModification(image.adId, userId))
}
