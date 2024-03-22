package com.holodome.repositories.redis

import cats.Functor
import com.holodome.config.types.JwtTokenExpiration
import com.holodome.domain.users.Username
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import com.holodome.ext.jwt.jwt._
import com.holodome.repositories.AuthedUserRepository

object RedisAuthedUserRepository {
  def make[F[_]: Functor](
      redis: RedisCommands[F, String, String],
      exp: JwtTokenExpiration
  ): AuthedUserRepository[F] = new RedisAuthedUserRepository[F](redis, exp)
}

sealed class RedisAuthedUserRepository[F[_]: Functor] private (
    redis: RedisCommands[F, String, String],
    exp: JwtTokenExpiration
) extends RedisDictionaryRepository[F, JwtToken, Username](redis, exp.value)
    with AuthedUserRepository[F]
