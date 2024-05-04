package com.holodome.modules

import cats.MonadThrow
import com.holodome.domain.services._
import com.holodome.effects.{Background, GenUUID, TimeSource}
import org.typelevel.log4cats.Logger
import com.holodome.interpreters.AdImageServiceInterpreter
import com.holodome.interpreters.AdServiceInterpreter
import com.holodome.interpreters.AdTagServiceInterpreter
import com.holodome.interpreters.AuthServiceInterpreter
import com.holodome.interpreters.ChatServiceInterpreter
import com.holodome.interpreters.FeedServiceInterpreter
import com.holodome.interpreters.IAMServiceInterpreter
import com.holodome.interpreters.MessageServiceInterpreter
import com.holodome.interpreters.TelemetryServiceBackgroundInterpreter
import com.holodome.interpreters.UserServiceInterpreter

sealed abstract class Services[F[_]] {
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdService[F]
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
  ): Services[F] =
    new Services[F] {
      val iam: IAMService[F] =
        IAMServiceInterpreter.make[F](repositories.ads, repositories.chats, repositories.images)
      val telemetry: TelemetryService[F] =
        TelemetryServiceBackgroundInterpreter.make(grpc.telemetry)
      override val users: UserService[F] =
        UserServiceInterpreter.make(repositories.users, iam)
      override val auth: AuthService[F] =
        AuthServiceInterpreter.make(
          repositories.users,
          infrastructure.jwtDict,
          infrastructure.usersDict,
          infrastructure.jwtTokens
        )
      override val ads: AdService[F] =
        AdServiceInterpreter
          .make[F](repositories.ads, repositories.tags, repositories.feed, iam, telemetry)
      override val chats: ChatService[F] =
        ChatServiceInterpreter.make[F](repositories.chats, repositories.ads, telemetry)
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
    }
}
