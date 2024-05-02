package com.holodome.infrastructure.minio

import cats.{Applicative, Monad, MonadThrow}
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all._
import com.holodome.ext.catsExt.liftCompletableFuture
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.{ObjectId, OBSUrl}
import io.minio._
import io.minio.errors.ErrorResponseException

import java.io.InputStream
import scala.util.control.NonFatal

object MinioObjectStorage {
  def make[F[_]: Async: MonadThrow](
      baseUrl: String,
      client: MinioAsyncClient,
      bucket: String
  ): F[ObjectStorage[F]] =
    ensureBucketIsCreated(client, bucket).as(new MinioObjectStorage(baseUrl, client, bucket))

  private def ensureBucketIsCreated[F[_]: Async: Monad](
      minio: MinioAsyncClient,
      bucket: String
  ): F[Unit] = liftCompletableFuture(
    minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
  ).map(scala.Boolean.unbox)
    .flatMap {
      case true => Applicative[F].unit
      case false =>
        liftCompletableFuture(
          minio.makeBucket(
            MakeBucketArgs
              .builder()
              .bucket(bucket)
              .build()
          )
        ).void
    }

}

private final class MinioObjectStorage[F[_]: Async: MonadThrow](
    baseUrl: String,
    client: MinioAsyncClient,
    bucket: String
) extends ObjectStorage[F] {

  override def putStream(id: ObjectId, blob: fs2.Stream[F, Byte], dataSize: Long): F[Unit] =
    fs2.io.toInputStreamResource(blob).use { is =>
      liftCompletableFuture {
        client
          .putObject(
            PutObjectArgs
              .builder()
              .bucket(bucket)
              .`object`(id.value)
              .contentType("binary/octet-stream")
              .stream(is, dataSize, -1)
              .build()
          )
      }.void
    }

  override def get(id: ObjectId): OptionT[F, fs2.Stream[F, Byte]] =
    OptionT(
      liftCompletableFuture(
        client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id.value).build())
      ).map(_.some).recoverWith {
        case NonFatal(e: ErrorResponseException) if e.errorResponse().code() == "NoSuchKey" =>
          none[GetObjectResponse].pure[F]
      }
    ).map { resp: InputStream =>
      fs2.io.readInputStream(resp.pure[F], 4096)
    }

  override def delete(id: ObjectId): F[Unit] =
    liftCompletableFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).void

  override def makeUrl(id: ObjectId): OBSUrl =
    OBSUrl(s"$baseUrl/$bucket/${id.value}")
}
