package maweituo
package infrastructure
package minio

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*

import maweituo.infrastructure.{OBSId, OBSUrl}

import _root_.org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.syntax.*

final case class MinioConnection(baseUrl: String, client: io.minio.MinioAsyncClient)

object MinioObjectStorage:
  def make[F[_]: Async: LoggerFactory](
      conn: MinioConnection,
      bucket: String
  ): F[ObjectStorage[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val client      = MinioClient[F](conn)
    ensureBucketIsCreated(client, bucket).as(MinioObjectStorage(client, bucket))

  private def ensureBucketIsCreated[F[_]: Async: Logger](
      minio: MinioClient[F],
      bucket: String
  ): F[Unit] =
    minio.bucketExists(bucket).flatMap {
      case true => info"Bucket $bucket already exists"
      case false =>
        for
          _ <- info"Bucket $bucket does not exist, creating"
          _ <- minio.createBucket(bucket)
          _ <- info"Bucket $bucket created"
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
      _ <- info"Putting blob $id of size $dataSize"
      x <- client.putStream(bucket, id, blob, dataSize)
      _ <- info"Finished putting blob $id"
    yield x

  override def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]] =
    client.get(bucket, id)

  override def delete(id: OBSId): F[Unit] =
    for
      _ <- info"Deleting blob $id"
      _ <- client.delete(bucket, id)
      _ <- info"Deleted blob $id"
    yield ()

  override def makeUrl(id: OBSId): OBSUrl =
    client.makeUrl(bucket, id)
