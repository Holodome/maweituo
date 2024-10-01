package maweituo.resources

import cats.Applicative
import cats.effect.{Resource, Sync}

import maweituo.config.MinioConfig

import io.minio.MinioAsyncClient

trait MkMinioClient[F[_]]:
  def newClient(c: MinioConfig): Resource[F, MinioAsyncClient]

object MkMinioClient:
  def apply[F[_]: MkMinioClient]: MkMinioClient[F] = summon

  given [F[_]: Sync]: MkMinioClient[F] = (cfg: MinioConfig) =>
    Resource.make[F, MinioAsyncClient](
      Sync[F].blocking(
        MinioAsyncClient
          .builder()
          .endpoint(s"http://${cfg.host}:${cfg.port}")
          .credentials(cfg.userId.value.toString(), cfg.password.value.toString())
          .build()
      )
    )(_ => Applicative[F].unit)
