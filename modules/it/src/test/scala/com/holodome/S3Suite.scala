package com.holodome

import cats.effect.IO
import cats.Show
import com.holodome.generators.{bigByteArrayGen, byteArrayGen, nonEmptyStringGen, objectIdGen}
import com.holodome.infrastructure.minio.MinioObjectStorage
import com.holodome.infrastructure.ObjectStorage.ObjectId
import io.minio.MinioAsyncClient
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.util.Random

object S3Suite extends SimpleIOSuite with Checkers {

  test("basic minio operations work") {
    val key   = ObjectId("test")
    val value = Random.nextBytes(1024)
    val minio = MinioAsyncClient
      .builder()
      .endpoint("http://localhost:9000")
      .credentials("minioadmin", "minioadmin")
      .build()
    for {
      storage <- MinioObjectStorage.make[IO](minio, "maweituo-test")
      x       <- storage.get(key).value
      _       <- storage.put(key, value)
      y       <- storage.get(key).value
      _       <- storage.delete(key)
    } yield expect.all(x.isEmpty, y.fold(false)(_ sameElements value))
  }
}
