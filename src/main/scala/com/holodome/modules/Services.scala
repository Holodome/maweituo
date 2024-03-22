package com.holodome.modules

import cats.MonadThrow
import cats.effect.Sync
import cats.syntax.all._
import com.holodome.auth.{JwtExpire, JwtTokens}
import com.holodome.config.types.AppConfig
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository
import com.holodome.services.{AuthService, UserService}

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
            AuthService.make(users, repositories.jwtRepository, tokens)
        }
      }
  }
}

sealed abstract class Services[F[_]] private {
  val users: UserService[F]
  val auth: AuthService[F]
}
