package com.holodome.modules

import cats.{MonadThrow, NonEmptyParallel}
import cats.effect.Async
import cats.syntax.all._
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.AppConfig
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.effects.Background
import com.holodome.infrastructure.minio.MinioObjectStorage
import com.holodome.infrastructure.redis.RedisEphemeralDict
import com.holodome.services._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient

sealed abstract class Services[F[_]] {
  val iam: IAMService[F]
  val telemetry: TelemetryService[F]
  val recs: RecommendationService[F]
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdvertisementService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
  val images: ImageService[F]
}

object Services {
  def make[F[_]: MonadThrow: Async: Background: NonEmptyParallel](
      repositories: Repositories[F],
      cfg: AppConfig,
      redis: RedisCommands[F, String, String],
      minio: MinioAsyncClient,
      grpc: GRPCClients[F]
  ): F[Services[F]] = {
    (
      JwtExpire
        .make[F]
        .map(JwtTokens.make[F](_, cfg.jwt)),
      MinioObjectStorage.make[F](minio, cfg.minio.bucket.value)
    )
      .mapN { case (tokens, imageStorage) =>
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
              users,
              RedisEphemeralDict
                .make[F](redis, cfg.jwt.tokenExpiration.value)
                .keyContramap[UserId](_.value.toString)
                .valueImap[JwtToken](JwtToken.apply, _.value),
              RedisEphemeralDict
                .make[F](redis, cfg.jwt.tokenExpiration.value)
                .keyContramap[JwtToken](_.value)
                .valueIFlatmap[UserId](Id.read[F, UserId], _.value.toString),
              tokens
            )
          override val ads: AdvertisementService[F] =
            AdvertisementService.make[F](repositories.ads, iam)
          override val chats: ChatService[F] =
            ChatService.make[F](repositories.chats, ads, telemetry)
          override val messages: MessageService[F] =
            MessageService.make[F](repositories.messages, iam)
          override val images: ImageService[F] =
            ImageService.make[F](
              repositories.images,
              ads,
              imageStorage,
              iam
            )

        }
      }
  }
}
