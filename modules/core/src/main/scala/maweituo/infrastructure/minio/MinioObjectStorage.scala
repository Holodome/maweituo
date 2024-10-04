package maweituo.infrastructure.minio
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.infrastructure.{OBSId, OBSUrl, ObjectStorage}

import _root_.org.typelevel.log4cats.Logger

final case class MinioConnection(baseUrl: String, client: io.minio.MinioAsyncClient)

object MinioObjectStorage:
  def make[F[_]: Async: Logger](
      conn: MinioConnection,
      bucket: String
  ): F[ObjectStorage[F]] =
    val client = new MinioClient[F](conn)
    ensureBucketIsCreated(client, bucket).as(new MinioObjectStorage(client, bucket))

  private def ensureBucketIsCreated[F[_]: Async: Logger](
      minio: MinioClient[F],
      bucket: String
  ): F[Unit] =
    minio.bucketExists(bucket).flatMap {
      case true => Logger[F].info(f"Bucket $bucket already exists")
      case false =>
        for
          _ <- Logger[F].info(f"Bucket $bucket does not exist, creating")
          _ <- minio.createBucket(bucket)
          _ <- Logger[F].info(f"Bucket $bucket created")
        yield ()
    }.recoverWith {
      case e => Logger[F].warn(e)("Error when creating bucket")
    }

private final class MinioObjectStorage[F[_]: Async: Logger](
    client: MinioClient[F],
    bucket: String
) extends ObjectStorage[F]:

  override def putStream(id: OBSId, blob: fs2.Stream[F, Byte], dataSize: Long): F[Unit] =
    for
      _ <- Logger[F].info(f"Putting blob $id of size $dataSize")
      x <- client.putStream(bucket, id, blob, dataSize)
      _ <- Logger[F].info(f"Finished putting blob $id")
    yield x

  override def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]] =
    client.get(bucket, id)

  override def delete(id: OBSId): F[Unit] =
    for
      _ <- Logger[F].info(f"Deleting blob $id")
      _ <- client.delete(bucket, id)
      _ <- Logger[F].info(f"Deleted blob $id")
    yield ()

  override def makeUrl(id: OBSId): OBSUrl =
    client.makeUrl(bucket, id)
