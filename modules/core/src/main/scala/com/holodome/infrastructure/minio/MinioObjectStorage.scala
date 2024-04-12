package com.holodome.infrastructure.minio

import _root_.org.apache.commons.io.IOUtils
import cats.{Applicative, Monad, MonadThrow}
import cats.data.OptionT
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.ext.cats.liftCompletableFuture
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import org.typelevel.log4cats.Logger
import io.minio._
import io.minio.errors.ErrorResponseException

import java.io.ByteArrayInputStream
import scala.util.control.NonFatal

object MinioObjectStorage {
  def make[F[_]: Async: MonadThrow](
      client: MinioAsyncClient,
      bucket: String
  ): F[MinioObjectStorage[F]] =
    ensureBucketIsCreated(client, bucket).map(_ => new MinioObjectStorage(client, bucket))

  private def ensureBucketIsCreated[F[_]: Async: Monad](
      minio: MinioAsyncClient,
      bucket: String
  ): F[Unit] = {
    liftCompletableFuture(
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

}

final class MinioObjectStorage[F[_]: Async: MonadThrow] private (
    client: MinioAsyncClient,
    bucket: String
) extends ObjectStorage[F] {

  override def put(id: ObjectId, blob: Array[Byte]): F[Unit] = {
    val res: Resource[F, ByteArrayInputStream] =
      Resource.make(Applicative[F].pure(new ByteArrayInputStream(blob))) { bais =>
        bais.close()
        Applicative[F].unit
      }
    res.use(bais =>
      liftCompletableFuture {
        client
          .putObject(
            PutObjectArgs
              .builder()
              .bucket(bucket)
              .`object`(id.value)
              .contentType("binary/octet-stream")
              .stream(bais, bais.available(), -1)
              .build()
          )
      }.void
    )
  }

  override def get(id: ObjectId): OptionT[F, Array[Byte]] =
    OptionT(
      liftCompletableFuture(
        client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id.value).build())
      ).map(IOUtils.toByteArray(_).some)
        .recoverWith {
          case NonFatal(e: ErrorResponseException) if e.errorResponse().code() == "NoSuchKey" =>
            Applicative[F].pure(None)
        }
    )

  override def delete(id: ObjectId): F[Unit] =
    liftCompletableFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).void
}
