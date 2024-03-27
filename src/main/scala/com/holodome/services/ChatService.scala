package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.messages.{Chat, ChatId, InvalidChatId}
import com.holodome.domain.users.UserId
import com.holodome.repositories.ChatRepository

trait ChatService[F[_]] {
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def find(chatId: ChatId): F[Chat]
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
    override def create(adId: AdId, clientId: UserId): F[ChatId] =
      adService
        .find(adId)
        .flatTap {
          case ad if ad.authorId === clientId =>
            CannotCreateChatWithMyself().raiseError[F, Unit]
          case _ => Monad[F].unit
        }
        .flatTap { ad =>
          chatRepo
            .findByAdAndClient(ad.id, clientId)
            .getOrElseF(ChatAlreadyExists().raiseError[F, Any])
        }
        .flatMap { ad =>
          chatRepo.create(adId, ad.authorId, clientId)
        }

    override def find(chatId: ChatId): F[Chat] =
      chatRepo
        .find(chatId)
        .getOrElseF(InvalidChatId().raiseError[F, Chat])
  }
}
