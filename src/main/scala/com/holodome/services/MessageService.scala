package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.instances.unit
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId
import com.holodome.repositories.{ChatRepository, MessageRepository}

trait MessageService[F[_]] {
  def createChat(adId: AdvertisementId, clientId: UserId): F[Unit]
  def findByAdvertisement(adId: AdvertisementId): F[List[Chat]]
  def findByClient(uid: UserId): F[List[Chat]]
  def findByAuthor(uid: UserId): F[List[Chat]]

  def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit]
  def history(chatId: ChatId, requester: UserId): F[HistoryResponse]
}

object MessageService {
  def make[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F]
  ): MessageService[F] =
    new MessageServiceInterpreter(msgRepo, chatRepo, adService)

  private final class MessageServiceInterpreter[F[_]: MonadThrow](
      msgRepo: MessageRepository[F],
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F]
  ) extends MessageService[F] {

    override def send(chatId: ChatId, senderId: UserId, req: SendMessageRequest): F[Unit] =
      findChatAndCheckAccess(chatId, senderId).flatMap {
        _ => msgRepo.send(chatId, senderId, req.text)
      }

    override def history(chatId: ChatId, requester: UserId): F[HistoryResponse] =
      findChatAndCheckAccess(chatId, requester)
        .flatMap(_ => msgRepo.chatHistory(chatId).map(HistoryResponse.apply))

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

    override def findByAdvertisement(adId: AdvertisementId): F[List[Chat]] = ???

    override def findByClient(uid: UserId): F[List[Chat]] = ???

    override def findByAuthor(uid: UserId): F[List[Chat]] = ???

    private def findChat(chatId: ChatId): F[Chat] =
      chatRepo
        .find(chatId)
        .getOrElseF(InvalidChatId().raiseError[F, Chat])

    private def findChatAndCheckAccess(chatId: ChatId, userId: UserId): F[Chat] =
      findChat(chatId).flatMap {
        case chat if userHasAccessToChat(chat, userId) =>
          ChatAccessForbidden().raiseError[F, Chat]
        case chat => Monad[F].pure(chat)
      }

    private def userHasAccessToChat(chat: Chat, user: UserId): Boolean =
      user === chat.adAuthor && user === chat.client
  }
}
