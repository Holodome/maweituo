package com.holodome.modules

import cats.MonadThrow
import cats.effect.Async
import com.holodome.effects.{Background, Clock, GenUUID}
import com.holodome.services._
import org.typelevel.log4cats.Logger

sealed abstract class Services[F[_]] {
  val iam: IAMService[F]
  val telemetry: TelemetryService[F]
  val recs: RecommendationService[F]
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdvertisementService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
  val images: AdImageService[F]
}

object Services {
  def make[F[_]: MonadThrow: GenUUID: Clock: Background: Logger](
      repositories: Repositories[F],
      infrastructure: Infrastructure[F],
      grpc: RecsClients[F]
  ): Services[F] = {
    new Services[F] {
      override val iam: IAMService[F] =
        IAMService.make[F](repositories.ads, repositories.chats, repositories.images)
      override val telemetry: TelemetryService[F] =
        TelemetryService.makeBackground(grpc.telemetry)
      override val recs: RecommendationService[F] =
        grpc.recs
      override val users: UserService[F] =
        UserService.make(repositories.users, iam)
      override val auth: AuthService[F] =
        AuthService.make(
          repositories.users,
          infrastructure.jwtDict,
          infrastructure.usersDict,
          infrastructure.jwtTokens
        )
      override val ads: AdvertisementService[F] =
        AdvertisementService.make[F](repositories.ads, repositories.tags, iam)
      override val chats: ChatService[F] =
        ChatService.make[F](repositories.chats, repositories.ads, telemetry)
      override val messages: MessageService[F] =
        MessageService.make[F](repositories.messages, iam)
      override val images: AdImageService[F] =
        AdImageService.make[F](
          repositories.images,
          repositories.ads,
          infrastructure.adImageStorage,
          iam
        )

    }
  }
}
