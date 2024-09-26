package com.holodome.modules

import com.holodome.domain.services.*
import com.holodome.effects.{ Background, GenUUID, TimeSource }
import com.holodome.interpreters.*

import cats.MonadThrow
import org.typelevel.log4cats.Logger

sealed abstract class Services[F[_]]:
  val users: UserService[F]
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
      repositories: Repositories[F],
      infrastructure: Infrastructure[F],
      grpc: RecsClients[F]
  ): Services[F] =
    new Services[F]:
      val iam: IAMService[F] =
        IAMServiceInterpreter.make[F](repositories.ads, repositories.chats, repositories.images)
      val telemetry: TelemetryService[F] =
        TelemetryServiceBackgroundInterpreter.make(grpc.telemetry)
      override val users: UserService[F] =
        UserServiceInterpreter.make(repositories.users, repositories.ads, iam)
      override val auth: AuthService[F] =
        AuthServiceInterpreter.make(
          repositories.users,
          infrastructure.jwtDict,
          infrastructure.usersDict,
          infrastructure.jwtTokens
        )
      override val ads: AdService[F] =
        AdServiceInterpreter
          .make[F](
            repositories.ads,
            repositories.tags,
            repositories.feed,
            iam,
            telemetry
          )
      override val chats: ChatService[F] =
        ChatServiceInterpreter.make[F](repositories.chats, repositories.ads, telemetry, iam)
      override val messages: MessageService[F] =
        MessageServiceInterpreter.make[F](repositories.messages, iam)
      override val images: AdImageService[F] =
        AdImageServiceInterpreter.make[F](
          repositories.images,
          repositories.ads,
          infrastructure.adImageStorage,
          iam
        )
      override val tags: AdTagService[F] = AdTagServiceInterpreter.make[F](repositories.tags)
      override val feed: FeedService[F] =
        FeedServiceInterpreter.make[F](repositories.feed, grpc.recs)
      override val recs: RecommendationService[F] = grpc.recs
