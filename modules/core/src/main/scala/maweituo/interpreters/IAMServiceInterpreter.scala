package maweituo.interpreters

import maweituo.domain.ads.AdId
import maweituo.domain.ads.images.ImageId
import maweituo.domain.ads.messages.{ Chat, ChatId }
import maweituo.domain.ads.repos.{ AdImageRepository, AdRepository, ChatRepository }
import maweituo.domain.errors.{ AdModificationForbidden, ChatAccessForbidden, UserModificationForbidden }
import maweituo.domain.services.IAMService
import maweituo.domain.users.UserId

import cats.syntax.all.*
import cats.{ Applicative, MonadThrow }

object IAMServiceInterpreter:
  def make[F[_]: MonadThrow](
      adRepo: AdRepository[F],
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
          case _                            => AdModificationForbidden(adId, userId).raiseError[F, Unit]
        }

    def authUserModification(target: UserId, userId: UserId): F[Unit] =
      (target === userId)
        .guard[Option]
        .fold(UserModificationForbidden(userId).raiseError[F, Unit])(Applicative[F].pure)

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor || user === chat.client

    def authImageDelete(imageId: ImageId, userId: UserId): F[Unit] =
      imageRepo
        .get(imageId)
        .flatMap(image => authAdModification(image.adId, userId))
