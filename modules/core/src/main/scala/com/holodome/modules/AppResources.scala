package com.holodome.modules

import com.holodome.config.AppConfig
import com.holodome.resources.{ MkHttpClient, MkMinioClient, MkRedisClient }

import cats.effect.{ Async, Resource }
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import io.minio.MinioAsyncClient

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val minio: MinioAsyncClient
)

object AppResources:
  def make[F[_]: MkRedisClient: MkMinioClient: MkHttpClient: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] =
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkMinioClient[F].newClient(cfg.minio)
    )
      .parMapN(new AppResources[F](_, _) {})
