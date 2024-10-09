package maweituo.infrastructure.minio

import java.io.InputStream

import scala.util.control.NonFatal

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.infrastructure.ext.catsExt.liftCompletableFuture
import maweituo.infrastructure.{OBSId, OBSUrl}

import io.minio.*
import io.minio.errors.ErrorResponseException

class MinioClient[F[_]: Async](connection: MinioConnection):

  def baseUrl: String          = connection.baseUrl
  def client: MinioAsyncClient = connection.client

  def putStream(bucket: String, id: OBSId, blob: fs2.Stream[F, Byte], dataSize: Long): F[Unit] =
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

  def get(bucket: String, id: OBSId): OptionT[F, fs2.Stream[F, Byte]] =
    OptionT(
      liftCompletableFuture(
        client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(id.value).build())
      ).map(_.some).recoverWith {
        case NonFatal(e: ErrorResponseException) if e.errorResponse().code() == "NoSuchKey" =>
          none[GetObjectResponse].pure[F]
      }
    ).map { (resp: InputStream) =>
      fs2.io.readInputStream(resp.pure[F], 4096)
    }

  def delete(bucket: String, id: OBSId): F[Unit] =
    liftCompletableFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).void

  def bucketExists(bucket: String): F[Boolean] =
    liftCompletableFuture(
      client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
    ).map(scala.Boolean.unbox)

  def createBucket(bucket: String): F[Unit] =
    liftCompletableFuture(
      client.makeBucket(
        MakeBucketArgs
          .builder()
          .bucket(bucket)
          .build()
      )
    ).void

  def makeUrl(bucket: String, id: OBSId): OBSUrl =
    OBSUrl(s"$baseUrl/$bucket/${id.value}")
