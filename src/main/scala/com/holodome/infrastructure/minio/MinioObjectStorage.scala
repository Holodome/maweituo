package com.holodome.infrastructure.minio

import cats.{Applicative, Monad}
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.ext.catsInterop.liftJavaFuture
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import io.minio.{GetObjectArgs, MinioAsyncClient, PutObjectArgs, RemoveObjectArgs}
import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream

object MinioObjectStorage {
  def make[F[_]: Async: Monad](client: MinioAsyncClient, bucket: String): MinioObjectStorage[F] =
    new MinioObjectStorage(client, bucket)
}

final class MinioObjectStorage[F[_]: Async: Monad] private (
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
              .stream(bais, bais.available(), 1)
              .build()
          )
      }.map(_ => ())
    )
  }

  override def get(id: ObjectId): F[Array[Byte]] =
    liftJavaFuture(
      client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).map(IOUtils.toByteArray(_))

  override def delete(id: ObjectId): F[Unit] =
    liftJavaFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).map(_ => ())
}
