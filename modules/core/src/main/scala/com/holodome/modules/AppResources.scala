package com.holodome.modules

import com.holodome.config.AppConfig
import com.holodome.resources.{MkHttpClient, MkMinioClient, MkPostgresClient, MkRedisClient}

import cats.effect.{Async, Resource}
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import doobie.Transactor
import io.minio.MinioAsyncClient

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val minio: MinioAsyncClient,
    val postgres: Transactor[F]
)

object AppResources:
  def make[F[_]: MkRedisClient: MkMinioClient: MkHttpClient: MkPostgresClient: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] =
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkMinioClient[F].newClient(cfg.minio),
      MkPostgresClient[F].newClient(cfg.postgres)
    )
      .parMapN(new AppResources[F](_, _, _) {})
