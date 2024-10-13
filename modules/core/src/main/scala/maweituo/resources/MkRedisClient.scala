package maweituo
package resources
import cats.effect.Resource
import cats.effect.kernel.Async
import cats.syntax.all.*

import maweituo.config.RedisConfig

import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import org.typelevel.log4cats.syntax.*
import org.typelevel.log4cats.{Logger, LoggerFactory}

trait MkRedisClient[F[_]]:
  def newClient(c: RedisConfig): Resource[F, RedisCommands[F, String, String]]

object MkRedisClient:
  def apply[F[_]: MkRedisClient]: MkRedisClient[F] = summon

  given [F[_]: Async: LoggerFactory]: MkRedisClient[F] = new:
    private given Logger[F] = LoggerFactory[F].getLogger
    given MkRedis[F]        = MkRedis.forAsync[F](using Async[F], dev.profunktor.redis4cats.log4cats.log4CatsInstance[F])

    private def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
      redis.info flatMap {
        _.get("redis_version").traverse_ { v =>
          info"Connected to redis $v"
        }
      }

    def newClient(cfg: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(s"redis://${cfg.host}").evalTap(checkRedisConnection)
