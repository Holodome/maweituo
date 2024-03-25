package com.holodome.repositories.redis

import cats.effect.Sync
import com.holodome.config.types.JwtTokenExpiration
import com.holodome.domain.users.UserId
import com.holodome.domain.Id
import com.holodome.repositories.AuthedUserRepository
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands

object RedisAuthedUserRepository {
  def make[F[_]: Sync](
      redis: RedisCommands[F, String, String],
      exp: JwtTokenExpiration
  ): AuthedUserRepository[F] = new RedisAuthedUserRepository[F](redis, exp)
}

sealed class RedisAuthedUserRepository[F[_]: Sync] private (
    redis: RedisCommands[F, String, String],
    exp: JwtTokenExpiration
) extends RedisDictionaryRepository[F, JwtToken, UserId](redis, exp.value)(
      jwt => jwt.value,
      uid => uid.value.toString,
      Id.read[F, UserId]
    )
    with AuthedUserRepository[F]
