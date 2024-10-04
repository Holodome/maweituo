package maweituo.modules

import cats.effect.{Async, Resource}
import cats.syntax.all.*

import maweituo.config.AppConfig
import maweituo.infrastructure.minio.MinioConnection
import maweituo.resources.{MkHttpClient, MkMinioClient, MkPostgresClient, MkRedisClient}

import dev.profunktor.redis4cats.RedisCommands
import doobie.Transactor

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val minio: MinioConnection,
    val postgres: Transactor[F]
)

object AppResources:
  def fromRaw[F[_]](
      redis: RedisCommands[F, String, String],
      minio: MinioConnection,
      postgres: Transactor[F]
  ): AppResources[F] = new AppResources[F](redis, minio, postgres) {}

  def make[F[_]: MkRedisClient: MkMinioClient: MkHttpClient: MkPostgresClient: Async](cfg: AppConfig)
      : Resource[F, AppResources[F]] =
    (
      MkRedisClient[F].newClient(cfg.redis),
      MkMinioClient[F].newClient(cfg.minio),
      MkPostgresClient[F].newClient(cfg.postgres)
    )
      .parMapN(new AppResources[F](_, _, _) {})
