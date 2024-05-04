package com.holodome.services.interpreters

import cats.Applicative
import cats.MonadThrow
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.CannotCreateChatWithMyself
import com.holodome.domain.errors.ChatAlreadyExists
import com.holodome.domain.messages.Chat
import com.holodome.domain.messages.ChatId
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID
import com.holodome.repositories.AdvertisementRepository
import com.holodome.repositories.ChatRepository
import com.holodome.services.ChatService
import com.holodome.services.TelemetryService
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
