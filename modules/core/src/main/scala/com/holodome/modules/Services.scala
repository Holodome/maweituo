package com.holodome.modules

import com.holodome.domain.ads.services.{ AdService, AdTagService, ChatService, MessageService }
import com.holodome.domain.services.*
import com.holodome.domain.users.services.{ AuthService, UserAdsService, UserService }
import com.holodome.effects.{ Background, GenUUID, TimeSource }
import com.holodome.interpreters.*
import com.holodome.interpreters.ads.*
import com.holodome.interpreters.users.*

import cats.MonadThrow
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
  val recs: RecommendationService[F]

object Services:
  def make[F[_]: MonadThrow: GenUUID: TimeSource: Background: Logger](
      repos: Repositories[F],
      infra: Infrastructure[F],
      grpc: RecsClients[F]
  ): Services[F] =
    new Services[F]:
      given iam: IAMService[F] =
        IAMServiceInterpreter.make[F](repos.ads, repos.chats, repos.images)
      given telemetry: TelemetryService[F]    = TelemetryServiceBackgroundInterpreter.make(grpc.telemetry)
      override val users: UserService[F]      = UserServiceInterpreter.make(repos.users)
      override val userAds: UserAdsService[F] = UserAdsServiceInterpreter.make(repos.ads)
      override val auth: AuthService[F] =
        AuthServiceInterpreter.make(repos.users, infra.jwtDict, infra.usersDict, infra.jwtTokens)
      override val ads: AdService[F]           = AdServiceInterpreter.make[F](repos.ads, repos.feed)
      override val chats: ChatService[F]       = ChatServiceInterpreter.make[F](repos.chats, repos.ads)
      override val messages: MessageService[F] = MessageServiceInterpreter.make[F](repos.messages)
      override val images: AdImageService[F] =
        AdImageServiceInterpreter.make[F](repos.images, repos.ads, infra.adImageStorage)
      override val tags: AdTagService[F]          = AdTagServiceInterpreter.make[F](repos.tags)
      override val feed: FeedService[F]           = FeedServiceInterpreter.make[F](repos.feed, grpc.recs)
      override val recs: RecommendationService[F] = grpc.recs
