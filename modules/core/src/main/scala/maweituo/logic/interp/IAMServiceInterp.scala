package maweituo
package logic
package interp

import cats.syntax.all.*
import cats.{Applicative, MonadThrow}

import maweituo.domain.all.*
object IAMServiceInterp:
  def make[F[_]: MonadThrow](
      adRepo: AdRepo[F],
      chatRepo: ChatRepo[F],
      imageRepo: AdImageRepo[F]
  ): IAMService[F] = new:
    def authChatAccess(chatId: ChatId)(using userId: Identity): F[Unit] =
      chatRepo
        .get(chatId)
        .flatMap {
          case chat if userHasAccessToChat(chat, userId) =>
            Applicative[F].unit
          case _ => DomainError.ChatAccessForbidden(chatId).raiseError[F, Unit]
        }

    def authAdModification(adId: AdId)(using userId: Identity): F[Unit] =
      adRepo
        .get(adId)
        .flatMap {
          case ad if ad.authorId === userId => Applicative[F].unit
          case _                            => DomainError.AdModificationForbidden(adId, userId).raiseError[F, Unit]
        }

    def authUserModification(target: UserId)(using userId: Identity): F[Unit] =
      (target === userId)
        .guard[Option]
        .fold(DomainError.UserModificationForbidden(userId).raiseError[F, Unit])(Applicative[F].pure)

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor || user === chat.client

    def authImageDelete(imageId: ImageId)(using userId: Identity): F[Unit] =
      imageRepo.get(imageId)
        .flatMap(image => authAdModification(image.adId))
