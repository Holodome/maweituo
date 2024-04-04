package com.holodome.resources

import cats.syntax.all._
import cats.effect.Resource
import cats.MonadThrow
import com.holodome.config.types.RedisConfig
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import org.typelevel.log4cats.Logger
import dev.profunktor.redis4cats.effect.MkRedis

trait MkRedisClient[F[_]] {
  def newClient(c: RedisConfig): Resource[F, RedisCommands[F, String, String]]
}

object MkRedisClient {
  def apply[F[_]: MkRedisClient]: MkRedisClient[F] = implicitly

  implicit def forAsyncLogger[F[_]: MkRedis: MonadThrow: Logger]: MkRedisClient[F] =
    new MkRedisClient[F] {
      private def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
        redis.info flatMap {
          _.get("redis_version").traverse_ { v =>
            Logger[F].info(s"Connected to redis $v")
          }
        }

      override def newClient(cfg: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
        Redis[F].utf8(s"redis://${cfg.host}").evalTap(checkRedisConnection)
    }
}
