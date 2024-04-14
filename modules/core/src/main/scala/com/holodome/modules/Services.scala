package com.holodome.modules

import cats.MonadThrow
import cats.effect.Async
import com.holodome.effects.{Background, TimeSource, GenUUID}
import com.holodome.services._
import org.typelevel.log4cats.Logger

sealed abstract class Services[F[_]] {
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdvertisementService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
  val images: AdImageService[F]
  val tags: AdTagService[F]
  val feed: FeedService[F]
}

object Services {
  def make[F[_]: MonadThrow: GenUUID: TimeSource: Background: Logger](
      repositories: Repositories[F],
      infrastructure: Infrastructure[F],
      grpc: RecsClients[F]
  ): Services[F] = {
    new Services[F] {
      val iam: IAMService[F] =
        IAMService.make[F](repositories.ads, repositories.chats, repositories.images)
      val telemetry: TelemetryService[F] =
        TelemetryService.makeBackground(grpc.telemetry)
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
        AdvertisementService.make[F](repositories.ads, repositories.tags, repositories.feed, iam)
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
      override val tags: AdTagService[F] = AdTagService.make[F](repositories.tags)
      override val feed: FeedService[F]  = FeedService.make[F](repositories.feed, grpc.recs)
    }
  }
}
