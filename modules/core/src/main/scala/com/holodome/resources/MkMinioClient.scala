package com.holodome.resources

import cats.effect.Resource
import cats.Applicative
import cats.effect.Sync
import com.holodome.config.types.MinioConfig
import io.minio.MinioAsyncClient

trait MkMinioClient[F[_]] {
  def newClient(c: MinioConfig): Resource[F, MinioAsyncClient]
}

object MkMinioClient {
  def apply[F[_]: MkMinioClient]: MkMinioClient[F] = implicitly

  implicit def forSync[F[_]: Sync]: MkMinioClient[F] = new MkMinioClient[F] {
    override def newClient(cfg: MinioConfig): Resource[F, MinioAsyncClient] =
      Resource.make[F, MinioAsyncClient](
        Sync[F].delay(
          MinioAsyncClient
            .builder()
            .endpoint(s"http://${cfg.host}:${cfg.port}")
            .credentials(cfg.userId.value.toString(), cfg.password.value.toString())
            .build()
        )
      )(_ => Applicative[F].unit)
  }
}
