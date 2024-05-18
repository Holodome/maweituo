package com.holodome.recs.modules

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all._
import com.holodome.config.types.RecsConfig
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.minio.MinioObjectStorage
import io.minio.MinioAsyncClient

sealed abstract class Infrastructure[F[_]] {
  val obs: ObjectStorage[F]
}

object Infrastructure {
  def make[F[_]: Async: MonadThrow](
      cfg: RecsConfig,
      minio: MinioAsyncClient
  ): F[Infrastructure[F]] =
    MinioObjectStorage
      .make[F](s"http://minio:${cfg.minio.port}", minio, cfg.minio.bucket.value)
      .map { minio =>
        new Infrastructure[F] {
          override val obs: ObjectStorage[F] = minio
        }
      }
}
