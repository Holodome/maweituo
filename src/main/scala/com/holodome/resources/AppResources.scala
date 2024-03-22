package com.holodome.resources

import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all._
import com.holodome.config.types.{AppConfig, RedisConfig}
import com.holodome.repositories.cassandra.CassandraResources
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.MkRedis
import org.typelevel.log4cats.Logger

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val cassandra: CassandraResources[F]
)

object AppResources {
  def make[F[_]: MkRedis: Concurrent: Logger: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    def checkRedisConnection(redis: RedisCommands[F, String, String]): F[Unit] =
      redis.info flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to redis $v")
        }
      }

    def mkRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection)

    (mkRedisResource(cfg.redisConfig), CassandraResources.make[F](cfg.cassandraConfig))
      .parMapN(new AppResources[F](_, _) {})
  }
}
