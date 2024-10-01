package maweituo.it

import scala.util.Random

import maweituo.infrastructure.OBSId
import maweituo.infrastructure.minio.MinioObjectStorage
import maweituo.tests.WeaverLogAdapter
import maweituo.tests.containers.makeMinioResource

import cats.effect.*
import cats.effect.kernel.Resource
import io.minio.MinioAsyncClient
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.Checkers

// object S3Suite extends IOSuite with Checkers:
  
//   type Res = MinioAsyncClient

//   override def sharedResource: Resource[IO, Res] = makeMinioResource[IO]

//   test("basic minio operations work") { (minio, log) =>
//     given Logger[IO] = new WeaverLogAdapter[IO](log)
//     val key         = OBSId("test")
//     val value       = Random.nextBytes(1024)
//     val valueStream = fs2.Stream.emits(value).covary[IO]
//     for
//       storage <- MinioObjectStorage.make[IO]("", minio, "maweituo")
//       x       <- storage.get(key).value
//       _       <- storage.putStream(key, valueStream, value.length)
//       y       <- storage.get(key).getOrRaise(new RuntimeException(""))
//       _       <- storage.delete(key)
//       d1      <- y.compile.toVector
//     yield expect.all(x.isEmpty, d1 sameElements value)
//   }
