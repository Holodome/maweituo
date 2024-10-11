package maweituo
package infrastructure
package redis

import scala.concurrent.duration.FiniteDuration

import dev.profunktor.redis4cats.RedisCommands

object RedisEphemeralDict:
  def make[F[_]: Monad](
      redis: RedisCommands[F, String, String],
      expire: FiniteDuration
  ): EphemeralDict[F, String, String] =
    new RedisEphemeralDict(redis, expire)

private final class RedisEphemeralDict[F[_]: Monad] private (
    redis: RedisCommands[F, String, String],
    expire: FiniteDuration
) extends EphemeralDict[F, String, String]:

  override def store(a: String, b: String): F[Unit] =
    redis.setEx(a, b, expire)

  override def delete(a: String): F[Unit] =
    redis.del(a).void

  override def get(a: String): OptionT[F, String] =
    OptionT(redis.get(a))
