package maweituo
package modules

import cats.MonadThrow

import maweituo.domain.all.*
import maweituo.infrastructure.effects.{Background, GenUUID, TimeSource}
import maweituo.logic.interp.all.*

import org.typelevel.log4cats.Logger
sealed abstract class Services[F[_]]:
  val users: UserService[F]
  val userAds: UserAdsService[F]
  val auth: AuthService[F]
  val ads: AdService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
  val images: AdImageService[F]
  val tags: AdTagService[F]
  val feed: FeedService[F]

object Services:
  def make[F[_]: MonadThrow: GenUUID: TimeSource: Background: Logger](
      repos: Repos[F],
      infra: Infrastructure[F]
  ): Services[F] =
    new Services[F]:
      given iam: IAMService[F] =
        IAMServiceInterp.make[F](repos.ads, repos.chats, repos.images)
      given telemetry: TelemetryService[F] =
        TelemetryServiceBackgroundInterp.make(TelemetryServiceInterp.make(repos.telemetry))
      override val users: UserService[F]      = UserServiceInterp.make(repos.users)
      override val userAds: UserAdsService[F] = UserAdsServiceInterp.make(repos.ads)
      override val auth: AuthService[F] =
        AuthServiceInterp.make(repos.users, infra.jwtDict, infra.usersDict, infra.jwtTokens)
      override val ads: AdService[F]           = AdServiceInterp.make[F](repos.ads)
      override val chats: ChatService[F]       = ChatServiceInterp.make[F](repos.chats, repos.ads)
      override val messages: MessageService[F] = MessageServiceInterp.make[F](repos.messages)
      override val images: AdImageService[F] =
        AdImageServiceInterp.make[F](repos.images, repos.ads, infra.adImageStorage)
      override val tags: AdTagService[F] = AdTagServiceInterp.make[F](repos.tags)
      override val feed: FeedService[F]  = FeedServiceInterp.make[F](repos.adSearch)
