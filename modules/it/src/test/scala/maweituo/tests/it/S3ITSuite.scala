package maweituo
package tests
package it

import scala.util.Random

import maweituo.infrastructure.{OBSId, ObjectStorage}
import maweituo.tests.containers.PartiallyAppliedMinio
import maweituo.tests.resources.*

import org.typelevel.log4cats.LoggerFactory
import weaver.*

class S3Suite(global: GlobalRead) extends ResourceSuite:

  type Res = PartiallyAppliedMinio

  override def sharedResource: Resource[IO, Res] = global.minio

  private def minioTest(name: String)(fn: ObjectStorage[IO] => F[Expectations]) =
    itTest(name) { minio0 =>
      minio0().use { minio =>
        fn(minio)
      }
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
