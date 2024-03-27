package com.holodome.modules

import cats.MonadThrow
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.AppConfig
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.infrastructure.redis.RedisEphemeralDict
import com.holodome.services._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands

sealed abstract class Services[F[_]] private {
  val iam: IAMService[F]
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdvertisementService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
  val images: ImageService[F]
}

object Services {
  def make[F[_]: MonadThrow: Sync](
      repositories: Repositories[F],
      cfg: AppConfig,
      redis: RedisCommands[F, String, String]
  ): F[Services[F]] = {
    JwtExpire
      .make[F]
      .map(JwtTokens.make[F](_, cfg.jwtAccessSecret.value, cfg.jwtTokenExpiration))
      .map { tokens =>
        new Services[F] {
          override val iam: IAMService[F] =
            IAMService.make[F](repositories.ads, repositories.chats, repositories.images)
          override val users: UserService[F] =
            UserService.make(repositories.users, iam)
          override val auth: AuthService[F] =
            AuthService.make(
              users,
              RedisEphemeralDict
                .make[F](redis, cfg.jwtTokenExpiration.value)
                .aContramap[UserId](_.value.toString)
                .bBimap[JwtToken](JwtToken.apply, _.value),
              RedisEphemeralDict
                .make[F](redis, cfg.jwtTokenExpiration.value)
                .aContramap[JwtToken](_.value)
                .bBiflatmap[UserId](Id.read[F, UserId], _.value.toString),
              tokens
            )
          override val ads: AdvertisementService[F] =
            AdvertisementService.make[F](repositories.ads, iam)
          override val chats: ChatService[F] = ChatService.make[F](repositories.chats, ads)
          override val messages: MessageService[F] =
            MessageService.make[F](repositories.messages, iam)
          override val images: ImageService[F] =
            ImageService.make[F](repositories.images, ads, ???, iam)
        }
      }
  }
}
