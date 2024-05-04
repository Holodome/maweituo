package com.holodome.services.interpreters

import cats.data.OptionT
import cats.syntax.all._
import cats.{Applicative, MonadThrow}
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.{CannotCreateChatWithMyself, ChatAlreadyExists}
import com.holodome.domain.messages.{Chat, ChatId}
import com.holodome.domain.repositories.{AdvertisementRepository, ChatRepository}
import com.holodome.domain.services.{ChatService, TelemetryService}
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import org.typelevel.log4cats.Logger

object ChatServiceInterpreter {

  def make[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepository[F],
      adRepo: AdvertisementRepository[F],
      telemetry: TelemetryService[F]
  ): ChatService[F] =
    new ChatServiceInterpreter(chatRepo, adRepo, telemetry)

}

private final class ChatServiceInterpreter[F[_]: MonadThrow: GenUUID: Logger](
    chatRepo: ChatRepository[F],
    adRepo: AdvertisementRepository[F],
    telemetry: TelemetryService[F]
) extends ChatService[F] {
  override def create(adId: AdId, clientId: UserId): F[ChatId] = for {
    _ <- chatRepo
      .findByAdAndClient(adId, clientId)
      .semiflatTap(_ => ChatAlreadyExists(adId, clientId).raiseError[F, Unit])
      .value
      .void
    author <- adRepo
      .get(adId)
      .map(_.authorId)
      .flatTap {
        case author if author === clientId =>
          CannotCreateChatWithMyself(adId, author).raiseError[F, Unit]
        case _ => Applicative[F].unit
      }
    id <- Id.make[F, ChatId]
    chat = Chat(
      id,
      adId,
      author,
      clientId
    )
    _ <- chatRepo.create(chat)
    _ <- telemetry.userDiscussed(clientId, adId)
    _ <- Logger[F].info(s"Created chat for ad $adId and user $clientId")
  } yield id

  override def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, ChatId] =
    chatRepo
      .findByAdAndClient(ad, user)
}
