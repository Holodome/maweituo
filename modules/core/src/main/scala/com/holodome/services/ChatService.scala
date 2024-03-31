package com.holodome.services

import cats.{Applicative, MonadThrow}
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.messages.{Chat, ChatId, InvalidChatId}
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.GenUUID
import com.holodome.repositories.ChatRepository

trait ChatService[F[_]] {
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def find(chatId: ChatId): F[Chat]
}

object ChatService {
  def make[F[_]: MonadThrow: GenUUID](
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F],
      telemetry: TelemetryService[F]
  ): ChatService[F] =
    new ChatServiceImpl(chatRepo, adService, telemetry)

  private final class ChatServiceImpl[F[_]: MonadThrow: GenUUID](
      chatRepo: ChatRepository[F],
      adService: AdvertisementService[F],
      telemetry: TelemetryService[F]
  ) extends ChatService[F] {
    override def create(adId: AdId, clientId: UserId): F[ChatId] = {
      chatRepo
        .findByAdAndClient(adId, clientId)
        .value
        .flatMap {
          case Some(_) => ChatAlreadyExists().raiseError[F, Unit]
          case None    => Applicative[F].unit
        } >>
        adService
          .find(adId)
          .map(_.authorId)
          .flatTap {
            case author if author === clientId =>
              CannotCreateChatWithMyself().raiseError[F, Unit]
            case _ => Applicative[F].unit
          }
          .flatMap { author =>
            for {
              id <- Id.make[F, ChatId]
              chat = Chat(
                id,
                adId,
                author,
                clientId
              )
              _ <- chatRepo.create(chat)
            } yield id
          }
    } <* telemetry.userDiscussed(clientId, adId)

    override def find(chatId: ChatId): F[Chat] =
      chatRepo
        .find(chatId)
        .getOrRaise(InvalidChatId())
  }
}
