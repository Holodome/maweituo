package maweituo
package logic
package interp
package ads

import cats.data.OptionT
import cats.syntax.all.*
import cats.{Applicative, MonadThrow}

import maweituo.domain.all.*
import maweituo.infrastructure.effects.GenUUID
import maweituo.utils.Id

import org.typelevel.log4cats.syntax.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

object ChatServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: LoggerFactory](
      chatRepo: ChatRepo[F],
      adRepo: AdRepo[F]
  )(using telemetry: TelemetryService[F], iam: IAMService[F]): ChatService[F] = new:
    given Logger[F] = LoggerFactory[F].getLogger

    def get(id: ChatId)(using Identity): F[Chat] =
      iam.authChatAccess(id) *> chatRepo.get(id)

    def create(adId: AdId)(using clientId: Identity): F[ChatId] =
      for
        _ <- chatRepo
          .findByAdAndClient(adId, clientId)
          .semiflatTap(_ =>
            warn"Chat for ad $adId with client $clientId already exists" *>
              DomainError.ChatAlreadyExists(adId, clientId).raiseError[F, Unit]
          )
          .value
          .void
        author <- adRepo
          .get(adId)
          .map(_.authorId)
          .flatTap {
            case author if author === clientId =>
              warn"User $author tried to creat chat with himsels" *>
                DomainError.CannotCreateChatWithMyself(adId, author).raiseError[F, Unit]
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
        _ <- info"Created chat for ad $adId and user $clientId"
      yield id

    def findForAdAndUser(ad: AdId)(using id: Identity): OptionT[F, Chat] =
      chatRepo
        .findByAdAndClient(ad, id)
        .flatTap { chat =>
          OptionT liftF iam.authChatAccess(chat.id)
        }

    def findForAd(ad: AdId)(using maweituo.domain.Identity): F[List[Chat]] =
      chatRepo
        .findForAd(ad)
        .flatTap { chats =>
          chats.traverse_(chat => iam.authChatAccess(chat.id))
        }
