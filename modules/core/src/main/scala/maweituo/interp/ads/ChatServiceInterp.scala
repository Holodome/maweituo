package maweituo.interp.ads

import maweituo.domain.Id
import maweituo.domain.ads.AdId
import maweituo.domain.ads.messages.{Chat, ChatId}
import maweituo.domain.ads.repos.{AdRepo, ChatRepo}
import maweituo.domain.ads.services.ChatService
import maweituo.domain.errors.{CannotCreateChatWithMyself, ChatAlreadyExists}
import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.domain.users.UserId
import maweituo.effects.GenUUID

import cats.data.OptionT
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}
import org.typelevel.log4cats.Logger

object ChatServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepo[F],
      adRepo: AdRepo[F]
  )(using telemetry: TelemetryService[F], iam: IAMService[F]): ChatService[F] = new:
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
