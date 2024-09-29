package com.holodome.resources

import com.holodome.config.RedisConfig

import cats.MonadThrow
import cats.effect.Resource
import cats.syntax.all.*
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.effect.MkRedis
import org.typelevel.log4cats.Logger

trait MkRedisClient[F[_]]:
  def newClient(c: RedisConfig): Resource[F, RedisCommands[F, String, String]]

object MkRedisClient:
  def apply[F[_]: MkRedisClient]: MkRedisClient[F] = summon

  given [F[_]: MkRedis: MonadThrow: Logger]: MkRedisClient[F] = new:
    private def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
      redis.info flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to redis $v")
        }
      }

    def newClient(cfg: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(s"redis://${cfg.host}").evalTap(checkRedisConnection)
