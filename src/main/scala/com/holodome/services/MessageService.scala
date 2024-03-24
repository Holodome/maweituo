package com.holodome.services

import cats.{Monad, MonadThrow}
import cats.instances.unit
import cats.syntax.all._
import com.holodome.domain.advertisements._
import com.holodome.domain.messages._
import com.holodome.domain.users.UserId
import com.holodome.repositories.{ChatRepository, MessageRepository}

trait MessageService[F[_]] {
  def find(id: ChatId): F[Chat]
  def createChat(adId: AdvertisementId, clientId: UserId): F[Unit]
  def findByAdvertisement(adId: AdvertisementId): F[List[Chat]]
  def findByClient(uid: UserId): F[List[Chat]]
  def findByAuthor(uid: UserId): F[List[Chat]]

  def send(message: Message): F[Unit]
  def history(chat: ChatId): F[List[Message]]
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

    override def send(message: Message): F[Unit] = ???

    override def history(chat: ChatId): F[List[Message]] = ???

    override def find(id: ChatId): F[Chat] = ???

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
            .getByAdAndClient(ad.id, clientId)
            .getOrElseF(ChatAlreadyExists().raiseError[F, Any])
        }
        .flatMap { ad =>
          chatRepo.create(adId, ad.authorId, clientId)
        }
        .map(_ => ())

    override def findByAdvertisement(adId: AdvertisementId): F[List[Chat]] = ???

    override def findByClient(uid: UserId): F[List[Chat]] = ???

    override def findByAuthor(uid: UserId): F[List[Chat]] = ???
  }
}
