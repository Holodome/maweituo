package com.holodome.services

import cats.{Monad, MonadThrow}
import com.holodome.domain.advertisements._
import com.holodome.domain.messages.{Chat, ChatAccessForbidden, ChatId, InvalidChatId}
import com.holodome.domain.users.UserId
import com.holodome.repositories.ChatRepository
import cats.syntax.all._

trait ChatService[F[_]] {
  def createChat(adId: AdvertisementId, clientId: UserId): F[Unit]
  def find(chatId: ChatId): F[Chat]
  def findChatAndCheckAccess(chatId: ChatId, userId: UserId): F[Chat]
}

object ChatService {
  def make[F[_]: MonadThrow](
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F]
  ): ChatService[F] =
    new ChatServiceImpl(chatRepo, adService)

  private final class ChatServiceImpl[F[_]: MonadThrow](
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F]
  ) extends ChatService[F] {
    override def createChat(adId: AdvertisementId, clientId: UserId): F[Unit] =
      adService
        .find(adId)
        .flatMap {
          case ad if ad.authorId === clientId =>
            CannotCreateChatWithMyself().raiseError[F, Advertisement]
          case ad => Monad[F].pure(ad)
        }
        .flatTap { ad =>
          chatRepo
            .findByAdAndClient(ad.id, clientId)
            .getOrElseF(ChatAlreadyExists().raiseError[F, Any])
        }
        .flatMap { ad =>
          chatRepo.create(adId, ad.authorId, clientId)
        }
        .map(_ => ())

    override def find(chatId: ChatId): F[Chat] =
      chatRepo
        .find(chatId)
        .getOrElseF(InvalidChatId().raiseError[F, Chat])

    override def findChatAndCheckAccess(chatId: ChatId, userId: UserId): F[Chat] =
      find(chatId).flatMap {
        case chat if userHasAccessToChat(chat, userId) =>
          ChatAccessForbidden().raiseError[F, Chat]
        case chat => Monad[F].pure(chat)
      }

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor && user === chat.client
  }
}
