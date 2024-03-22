package com.holodome.repositories.redis

import cats.data.OptionT
import com.holodome.config.types.JwtTokenExpiration
import com.holodome.domain.users
import dev.profunktor.auth.jwt
import dev.profunktor.redis4cats.RedisCommands
import cats.syntax.all._
import cats._
import com.holodome.repositories.JwtRepository

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
) extends JwtRepository[F] {

  override def storeToken(username: users.Username, token: jwt.JwtToken): F[Unit] =
    redis.setEx(username.value, token.value, exp.value)

  override def getToken(username: users.Username): OptionT[F, jwt.JwtToken] =
    OptionT(redis.get(username.value)).map(jwt.JwtToken)

  override def deleteToken(username: users.Username): F[Unit] =
    redis.del(username.value).map(_ => ())
}
