package maweituo.infrastructure.minio

import java.io.InputStream

import scala.util.control.NonFatal

import maweituo.ext.catsExt.liftCompletableFuture
import maweituo.infrastructure.{OBSId, OBSUrl, ObjectStorage}

import _root_.org.typelevel.log4cats.Logger
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import io.minio.*
import io.minio.errors.ErrorResponseException

object MinioObjectStorage:
  def make[F[_]: Async: Logger](
      baseUrl: String,
      client: MinioAsyncClient,
      bucket: String
  ): F[ObjectStorage[F]] =
    ensureBucketIsCreated(client, bucket).as(new MinioObjectStorage(baseUrl, client, bucket))

  private def ensureBucketIsCreated[F[_]: Async: Logger](
      minio: MinioAsyncClient,
      bucket: String
  ): F[Unit] = liftCompletableFuture(
    minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
  ).map(scala.Boolean.unbox)
    .flatMap {
      case true => Logger[F].info(f"Bucket $bucket already exists")
      case false =>
        Logger[F].info(f"Bucket $bucket does not exist, creating") *> liftCompletableFuture(
          minio.makeBucket(
            MakeBucketArgs
              .builder()
              .bucket(bucket)
              .build()
          )
        ).void *> Logger[F].info(f"Bucket $bucket created")
    }

private final class MinioObjectStorage[F[_]: Async: Logger](
    baseUrl: String,
    client: MinioAsyncClient,
    bucket: String
) extends ObjectStorage[F]:

  override def putStream(id: OBSId, blob: fs2.Stream[F, Byte], dataSize: Long): F[Unit] =
    Logger[F].info(f"Putting blob $id of size $dataSize") *> fs2.io.toInputStreamResource(blob).use { is =>
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
    } <* Logger[F].info(f"Finished putting blob $id")

  override def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]] =
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

  override def delete(id: OBSId): F[Unit] =
    Logger[F].info(f"Deleting blob $id") *> liftCompletableFuture(
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(id.value).build())
    ).void <* Logger[F].info(f"Deleted blob $id")

  override def makeUrl(id: OBSId): OBSUrl =
    OBSUrl(s"$baseUrl/$bucket/${id.value}")
