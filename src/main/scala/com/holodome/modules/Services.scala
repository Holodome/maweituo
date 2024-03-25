package com.holodome.modules

import cats.MonadThrow
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.AppConfig
import com.holodome.services.{
  AdvertisementService,
  AuthService,
  ChatService,
  MessageService,
  UserService
}

sealed abstract class Services[F[_]] private {
  val users: UserService[F]
  val auth: AuthService[F]
  val ads: AdvertisementService[F]
  val chats: ChatService[F]
  val messages: MessageService[F]
}

object Services {
  def make[F[_]: MonadThrow: Sync](
      repositories: Repositories[F],
      cfg: AppConfig
  ): F[Services[F]] = {
    JwtExpire
      .make[F]
      .map(JwtTokens.make[F](_, cfg.jwtAccessSecret.value, cfg.jwtTokenExpiration))
      .map { tokens =>
        new Services[F] {
          override val users: UserService[F] =
            UserService.make(repositories.userRepository)
          override val auth: AuthService[F] =
            AuthService.make(
              users,
              repositories.jwtRepository,
              repositories.authedUserRepository,
              tokens
            )
          override val ads: AdvertisementService[F] =
            AdvertisementService.make[F](repositories.advertisementRepository)
          override val chats: ChatService[F] = ChatService.make[F](repositories.chatRepository, ads)
          override val messages: MessageService[F] =
            MessageService.make[F](repositories.messageRepository, chats)
        }
      }
  }
}
