package com.holodome.resources

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.config.types.AppConfig
import com.ringcentral.cassandra4io.CassandraSession
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val cassandra: CassandraSession[F],
    val minio: MinioAsyncClient
)

object AppResources {
  def make[F[_]: MkRedisClient: MkMinioClient: MkCassandraClient: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkCassandraClient[F].newClient(cfg.cassandra),
      MkMinioClient[F].newClient(cfg.minio)
    )
      .parMapN(new AppResources[F](_, _, _) {})
  }
}
