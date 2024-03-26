package com.holodome.infrastructure.redis

import cats.Monad
import cats.data.OptionT
import cats.syntax.all._
import com.holodome.infrastructure.EphemeralDict
import dev.profunktor.redis4cats.RedisCommands

import scala.concurrent.duration.FiniteDuration

final class RedisEphemeralDict[F[_]: Monad](
    redis: RedisCommands[F, String, String],
    expire: FiniteDuration
) extends EphemeralDict[F, String, String] {

  override def store(a: String, b: String): F[Unit] =
    redis.setEx(a, b, expire)

  override def delete(a: String): F[Unit] =
    redis.get(a).map(_ => ())

  override def get(a: String): OptionT[F, String] =
    OptionT(redis.get(a))
}

object RedisEphemeralDict {
  def make[F[_]: Monad](
      redis: RedisCommands[F, String, String],
      expire: FiniteDuration
  ): EphemeralDict[F, String, String] =
    new RedisEphemeralDict(redis, expire)
}
