package maweituo
package logic
package interp
package ads

import maweituo.domain.all.*
import maweituo.infrastructure.effects.GenUUID
import maweituo.utils.Id

import org.typelevel.log4cats.Logger

object ChatServiceInterp:
  def make[F[_]: MonadThrow: GenUUID: Logger](
      chatRepo: ChatRepo[F],
      adRepo: AdRepo[F]
  )(using telemetry: TelemetryService[F], iam: IAMService[F]): ChatService[F] = new:
    def get(id: ChatId)(using Identity): F[Chat] =
      iam.authChatAccess(id) *> chatRepo.get(id)

    def create(adId: AdId)(using clientId: Identity): F[ChatId] =
      for
        _ <- chatRepo
          .findByAdAndClient(adId, clientId)
          .semiflatTap(_ =>
            Logger[F].warn(
              s"Chat for ad $adId with client $clientId already exists"
            ) *> DomainError.ChatAlreadyExists(adId, clientId).raiseError[F, Unit]
          )
          .value
          .void
        author <- adRepo
          .get(adId)
          .map(_.authorId)
          .flatTap {
            case author if author === clientId =>
              Logger[F].warn(
                s"User $author tried to creat chat with himsels"
              ) *> DomainError.CannotCreateChatWithMyself(adId, author).raiseError[F, Unit]
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

    def findForAdAndUser(ad: AdId)(using id: Identity): OptionT[F, Chat] =
      chatRepo
        .findByAdAndClient(ad, id)
        .flatTap { chat =>
          OptionT liftF iam.authChatAccess(chat.id)
        }
