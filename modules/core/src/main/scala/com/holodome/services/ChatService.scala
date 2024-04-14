package com.holodome.services

import cats.{Applicative, MonadThrow}
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.domain.ads._
import com.holodome.domain.messages.{Chat, ChatId}
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.domain.errors._
import com.holodome.effects.GenUUID
import com.holodome.repositories.{AdvertisementRepository, ChatRepository}
import org.typelevel.log4cats.Logger

trait ChatService[F[_]] {
  def create(adId: AdId, clientId: UserId): F[ChatId]
  def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, ChatId]
}

object ChatService {
  def make[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepository[F],
      adRepo: AdvertisementRepository[F],
      telemetry: TelemetryService[F]
  ): ChatService[F] =
    new ChatServiceImpl(chatRepo, adRepo, telemetry)

  private final class ChatServiceImpl[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepository[F],
      adRepo: AdvertisementRepository[F],
      telemetry: TelemetryService[F]
  ) extends ChatService[F] {
    override def create(adId: AdId, clientId: UserId): F[ChatId] = {
      chatRepo
        .findByAdAndClient(adId, clientId)
        .semiflatTap(_ => ChatAlreadyExists().raiseError[F, Unit])
        .value
        .void *>
        adRepo
          .get(adId)
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
          } <* Logger[F].info(s"Created chat for ad $adId and user $clientId")
    } <* telemetry.userDiscussed(clientId, adId)

    override def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, ChatId] =
      chatRepo
        .findByAdAndClient(ad, user)
  }
}
