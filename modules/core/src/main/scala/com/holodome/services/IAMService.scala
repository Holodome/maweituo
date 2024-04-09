package com.holodome.services

import cats.{Applicative, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.images.{ImageId, InvalidImageId}
import com.holodome.domain.messages._
import com.holodome.domain.users.{InvalidAccess, UserId}
import com.holodome.repositories.{AdvertisementRepository, ChatRepository, ImageRepository}

trait IAMService[F[_]] {
  def authorizeChatAccess(chatId: ChatId, userId: UserId): F[Unit]
  def authorizeAdModification(advertisementId: AdId, userId: UserId): F[Unit]
  def authorizeUserModification(target: UserId, userId: UserId): F[Unit]
  def authorizeImageDelete(imageId: ImageId, userId: UserId): F[Unit]
}

object IAMService {
  def make[F[_]: MonadThrow](
      adRepo: AdvertisementRepository[F],
      chatRepo: ChatRepository[F],
      imageRepo: ImageRepository[F]
  ): IAMService[F] =
    new IAMServiceInterpreter(adRepo, chatRepo, imageRepo)

  private final class IAMServiceInterpreter[F[_]: MonadThrow](
      adRepo: AdvertisementRepository[F],
      chatRepo: ChatRepository[F],
      imageRepo: ImageRepository[F]
  ) extends IAMService[F] {

    override def authorizeChatAccess(chatId: ChatId, userId: UserId): F[Unit] =
      chatRepo
        .find(chatId)
        .getOrRaise(InvalidChatId())
        .flatMap {
          case chat if userHasAccessToChat(chat, userId) =>
            Applicative[F].unit
          case _ => ChatAccessForbidden().raiseError[F, Unit]
        }

    override def authorizeAdModification(advertisementId: AdId, userId: UserId): F[Unit] =
      adRepo
        .find(advertisementId)
        .getOrRaise(InvalidAdId(advertisementId))
        .flatMap {
          case ad if ad.authorId === userId => Applicative[F].unit
          case _                            => NotAnAuthor().raiseError[F, Unit]
        }

    override def authorizeUserModification(target: UserId, userId: UserId): F[Unit] =
      (target === userId)
        .guard[Option]
        .fold(InvalidAccess().raiseError[F, Unit])(_ => Applicative[F].unit)

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor || user === chat.client

    override def authorizeImageDelete(imageId: ImageId, userId: UserId): F[Unit] =
      imageRepo
        .getMeta(imageId)
        .getOrRaise(InvalidImageId())
        .flatMap(image => authorizeAdModification(image.adId, userId))
  }
}
