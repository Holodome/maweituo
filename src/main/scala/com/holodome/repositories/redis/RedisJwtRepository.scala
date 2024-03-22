package com.holodome.repositories.redis

import com.holodome.config.types.JwtTokenExpiration
import dev.profunktor.redis4cats.RedisCommands
import cats._
import com.holodome.domain.users.Username
import com.holodome.repositories.JwtRepository
import dev.profunktor.auth.jwt.JwtToken
import com.holodome.ext.jwt.jwt._

object RedisJwtRepository {
  def make[F[_]: Functor](
      redis: RedisCommands[F, String, String],
      exp: JwtTokenExpiration
  ): JwtRepository[F] =
    new RedisJwtRepository[F](redis, exp)
}

sealed class RedisJwtRepository[F[_]: Functor] private (
    redis: RedisCommands[F, String, String],
    exp: JwtTokenExpiration
) extends RedisDictionaryRepository[F, Username, JwtToken](redis, exp.value)
    with JwtRepository[F]
