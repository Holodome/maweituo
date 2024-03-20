package com.holodome.modules

import cats.MonadThrow
import cats.effect.Sync
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository
import com.holodome.services.UserService

object Services {
  def make[F[_]: MonadThrow: Sync](
      repositories: Repositories[F]
  ): Services[F] = {
    new Services[F] {
      override val users: UserService[F] =
        UserService.make(repositories.userRepository)
    }
  }
}

sealed abstract class Services[F[_]] private {
  val users: UserService[F]
}
