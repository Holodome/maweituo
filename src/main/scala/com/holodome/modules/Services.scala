package com.holodome

import cats.MonadThrow
import com.holodome.effects.GenUUID
import com.holodome.repositories.UserRepository
import com.holodome.services.UserService

object Services {
  def make[F[_]: MonadThrow: GenUUID](
      userRepo: UserRepository[F]
  ): Services[F] = {
    new Services[F] {
      override val users: UserService[F] = UserService.make(userRepo)
    }
  }
}

sealed abstract class Services[F[_]] private {
  val users: UserService[F]
}
