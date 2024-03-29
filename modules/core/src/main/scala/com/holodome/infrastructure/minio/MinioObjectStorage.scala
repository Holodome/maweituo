package com.holodome.infrastructure.minio

import cats.{Applicative, Monad, MonadThrow}
import cats.data.OptionT
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.ext.cats.liftJavaFuture
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import io.minio.{
  BucketExistsArgs,
  GetObjectArgs,
  MakeBucketArgs,
  MinioAsyncClient,
  PutObjectArgs,
  RemoveObjectArgs
}
import io.minio.errors.ErrorResponseException
import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream
import java.util.concurrent.CompletionException
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
    liftJavaFuture(
      minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
    ).map(scala.Boolean.unbox)
      .flatMap {
        case true => Applicative[F].unit
        case false =>
          liftJavaFuture(
            minio.makeBucket(
              MakeBucketArgs
                .builder()
                .bucket(bucket)
                .build()
            )
          ).map(_ => ())
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
      liftJavaFuture {
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
      }.map(_ => ())
    )
  }

  override def get(id: ObjectId): OptionT[F, Array[Byte]] =
    OptionT(
      liftJavaFuture(
        client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id.value).build())
      ).map(IOUtils.toByteArray(_).some)
        .recoverWith {
          case NonFatal(e: ErrorResponseException) if e.errorResponse().code() == "NoSuchKey" =>
            Applicative[F].pure(None)
        }
    )

  override def delete(id: ObjectId): F[Unit] =
    liftJavaFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).map(_ => ())
}
