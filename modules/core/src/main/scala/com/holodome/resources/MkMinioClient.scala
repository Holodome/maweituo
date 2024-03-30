package com.holodome.resources

import cats.effect.Resource
import cats.Applicative
import cats.effect.kernel.Sync
import com.holodome.config.types.MinioConfig
import io.minio.MinioAsyncClient

trait MkMinioClient[F[_]] {
  def newClient(c: MinioConfig): Resource[F, MinioAsyncClient]
}

object MkMinioClient {
  def apply[F[_]: MkMinioClient]: MkMinioClient[F] = implicitly

  implicit def forSync[F[_]: Sync]: MkMinioClient[F] = new MkMinioClient[F] {
    override def newClient(c: MinioConfig): Resource[F, MinioAsyncClient] =
      Resource.make[F, MinioAsyncClient](
        Sync[F].delay(
          MinioAsyncClient
            .builder()
            .endpoint(c.endpoint)
            .credentials(c.userId.value.toString(), c.password.value.toString())
            .build()
        )
      )(_ => Applicative[F].unit)
  }
}
