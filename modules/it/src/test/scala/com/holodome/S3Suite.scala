package com.holodome

import cats.effect.IO
import cats.Show
import com.holodome.generators.{bigByteArrayGen, byteArrayGen, nonEmptyStringGen, objectIdGen}
import com.holodome.infrastructure.minio.MinioObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import io.minio.MinioAsyncClient
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.util.Random

object S3Suite extends SimpleIOSuite with Checkers {

  test("basic minio operations work") {
    forall(objectIdGen) { key =>
      val value       = Random.nextBytes(1024)
      val valueStream = fs2.Stream.emits(value).covary[F]
      val minio = MinioAsyncClient
        .builder()
        .endpoint("http://localhost:9000")
        .credentials("minioadmin", "minioadmin")
        .build()
      for {
        storage <- MinioObjectStorage.make[IO]("", minio, "maweituo-test")
        x       <- storage.get(key).value
        _       <- storage.putStream(key, valueStream, value.length)
        y       <- storage.get(key).getOrRaise(new RuntimeException(""))
        _       <- storage.delete(key)
        d1      <- y.compile.toVector
      } yield expect.all(x.isEmpty, d1 sameElements value)
    }
  }
}
