package com.holodome.repositories.redis

import cats._
import com.holodome.config.types.JwtTokenExpiration
import com.holodome.domain.users._
import com.holodome.repositories.JwtRepository
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands

object RedisJwtRepository {
  def make[F[_]: Monad](
      redis: RedisCommands[F, String, String],
      exp: JwtTokenExpiration
  ): JwtRepository[F] =
    new RedisJwtRepository[F](redis, exp)
}

sealed class RedisJwtRepository[F[_]: Monad] private (
    redis: RedisCommands[F, String, String],
    exp: JwtTokenExpiration
) extends RedisDictionaryRepository[F, UserId, JwtToken](redis, exp.value)(
      _.value.toString,
      _.value,
      str => Monad[F].pure(JwtToken(str))
    )
    with JwtRepository[F]
