package com.holodome.interpreters

import com.holodome.domain.Id
import com.holodome.domain.ads.AdId
import com.holodome.domain.errors.{ CannotCreateChatWithMyself, ChatAlreadyExists }
import com.holodome.domain.messages.{ Chat, ChatId }
import com.holodome.domain.repositories.{ AdRepository, ChatRepository }
import com.holodome.domain.services.{ ChatService, IAMService, TelemetryService }
import com.holodome.domain.users.UserId
import com.holodome.effects.GenUUID

import cats.data.OptionT
import cats.syntax.all.*
import cats.{ Applicative, MonadThrow }
import org.typelevel.log4cats.Logger

object ChatServiceInterpreter:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepository[F],
      adRepo: AdRepository[F],
      telemetry: TelemetryService[F],
      iam: IAMService[F]
  ): ChatService[F] = new:
    def get(id: ChatId, requester: UserId): F[Chat] =
      iam.authChatAccess(id, requester) *> chatRepo.get(id)

    def create(adId: AdId, clientId: UserId): F[ChatId] =
      for
        _ <- chatRepo
          .findByAdAndClient(adId, clientId)
          .semiflatTap(_ =>
            Logger[F].warn(
              s"Chat for ad $adId with client $clientId already exists"
            ) *> ChatAlreadyExists(adId, clientId).raiseError[F, Unit]
          )
          .value
          .void
        author <- adRepo
          .get(adId)
          .map(_.authorId)
          .flatTap {
            case author if author === clientId =>
              Logger[F].warn(
                s"User $author tried to creat chat with himself"
              ) *> CannotCreateChatWithMyself(adId, author).raiseError[F, Unit]
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
      yield id

    def findForAdAndUser(ad: AdId, user: UserId): OptionT[F, Chat] =
      chatRepo
        .findByAdAndClient(ad, user)
        .flatTap { chat =>
          OptionT liftF iam.authChatAccess(chat.id, user)
        }
