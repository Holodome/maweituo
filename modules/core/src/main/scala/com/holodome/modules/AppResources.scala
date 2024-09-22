package com.holodome.modules

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.config.types.AppConfig
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient
import com.holodome.resources.{MkRedisClient, MkHttpClient, MkMinioClient}

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val minio: MinioAsyncClient
)

object AppResources {
  def make[F[_]: MkRedisClient: MkMinioClient: MkHttpClient: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkMinioClient[F].newClient(cfg.minio)
    )
      .parMapN(new AppResources[F](_, _) {})
  }
}
