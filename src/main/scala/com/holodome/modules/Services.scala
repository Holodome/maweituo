package com.holodome.modules

import cats.MonadThrow
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository
import com.holodome.services.UserService

object Services {
  def make[F[_]: MonadThrow: GenUUID](
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
