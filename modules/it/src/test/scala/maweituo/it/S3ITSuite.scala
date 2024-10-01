package maweituo.it

import scala.util.Random

import cats.data.OptionT
import cats.effect.*
import cats.effect.kernel.Resource

import maweituo.infrastructure.OBSId
import maweituo.infrastructure.minio.MinioObjectStorage
import maweituo.tests.WeaverLogAdapter
import maweituo.tests.containers.*

import io.minio.MinioAsyncClient
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.Checkers

object S3Suite extends IOSuite with Checkers:

  type Res = MinioAsyncClient

  override def sharedResource: Resource[IO, Res] = makeMinioResource[IO]

  test("get invalid") { (minio, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val key          = OBSId("test")
    for
      storage <- MinioObjectStorage.make[IO]("", minio, "maweituo")
      x       <- storage.get(key).value
    yield expect.same(None, x)
  }

  test("put and get") { (minio, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val key          = OBSId("test1")
    val value        = Random.nextBytes(1024).toList
    for
      storage <- MinioObjectStorage.make[IO]("", minio, "maweituo")
      _       <- storage.put(key, value)
      x       <- storage.get(key).flatMap(v => OptionT.liftF(v.compile.toList)).value
    yield expect.same(Some(value), x)
  }

  test("put and get stream") { (minio, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val key          = OBSId("test2")
    val value        = Random.nextBytes(1024).toList
    val valueStream  = fs2.Stream.emits(value).covary[IO]
    for
      storage <- MinioObjectStorage.make[IO]("", minio, "maweituo")
      _       <- storage.putStream(key, valueStream, value.length)
      x       <- storage.get(key).flatMap(v => OptionT.liftF(v.compile.toList)).value
    yield expect.same(Some(value), x)
  }

  test("delete") { (minio, log) =>
    given Logger[IO] = new WeaverLogAdapter[IO](log)
    val key          = OBSId("test3")
    val value        = Random.nextBytes(1024).toList
    val valueStream  = fs2.Stream.emits(value).covary[IO]
    for
      storage <- MinioObjectStorage.make[IO]("", minio, "maweituo")
      _       <- storage.putStream(key, valueStream, value.length)
      _       <- storage.delete(key)
      x       <- storage.get(key).value
    yield expect.same(None, x)
  }
