package com.holodome.repositories.minio

import cats.{Applicative, Monad}
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.holodome.ext.catsInterop.liftJavaFuture
import io.minio.{GetObjectArgs, MinioAsyncClient, PutObjectArgs, RemoveObjectArgs}
import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream

class MinioStorage[F[_]: Async: Monad](client: MinioAsyncClient) {

  def put(bucket: String, id: String, blob: Array[Byte]): F[Unit] = {
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
              .`object`(id)
              .contentType("binary/octet-stream")
              .stream(bais, bais.available(), 1)
              .build()
          )
      }.map(_ => ())
    )
  }

  def get(bucket: String, id: String): F[Array[Byte]] =
    liftJavaFuture(
      client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id).build())
    ).map(IOUtils.toByteArray(_))

  def delete(bucket: String, id: String): F[Unit] =
    liftJavaFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id).build())
    ).map(_ => ())
}
