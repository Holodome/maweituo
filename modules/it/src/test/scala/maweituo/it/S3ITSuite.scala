package maweituo.it

import scala.util.Random

import cats.data.OptionT
import cats.effect.*
import cats.effect.kernel.Resource

import maweituo.infrastructure.minio.{MinioConnection, MinioObjectStorage}
import maweituo.infrastructure.{OBSId, ObjectStorage}
import maweituo.it.resources.*
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import org.typelevel.log4cats.Logger
import weaver.*

class S3Suite(global: GlobalRead) extends ResourceSuite:

  type Res = MinioConnection

  override def sharedResource: Resource[IO, Res] = global.minio

  private def minioTest(name: String)(fn: ObjectStorage[IO] => F[Expectations]) =
    test(name) { (minio, log) =>
      given Logger[IO] = new WeaverLogAdapter[IO](log)
      MinioObjectStorage.make[IO](minio, "maweituo").flatMap(fn)
    }

  minioTest("get invalid") { storage =>
    val key = OBSId("test")
    for
      x <- storage.get(key).value
    yield expect.same(None, x)
  }

  minioTest("put and get") { storage =>
    val key   = OBSId("test1")
    val value = Random.nextBytes(1024).toList
    for
      _ <- storage.put(key, value)
      x <- storage.get(key).flatMap(v => OptionT.liftF(v.compile.toList)).value
    yield expect.same(Some(value), x)
  }

  minioTest("put and get stream") { storage =>
    val key         = OBSId("test2")
    val value       = Random.nextBytes(1024).toList
    val valueStream = fs2.Stream.emits(value).covary[IO]
    for
      _ <- storage.putStream(key, valueStream, value.length)
      x <- storage.get(key).flatMap(v => OptionT.liftF(v.compile.toList)).value
    yield expect.same(Some(value), x)
  }

  minioTest("delete") { storage =>
    val key         = OBSId("test3")
    val value       = Random.nextBytes(1024).toList
    val valueStream = fs2.Stream.emits(value).covary[IO]
    for
      _ <- storage.putStream(key, valueStream, value.length)
      _ <- storage.delete(key)
      x <- storage.get(key).value
    yield expect.same(None, x)
  }
