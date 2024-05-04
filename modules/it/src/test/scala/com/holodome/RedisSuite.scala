package com.holodome

import cats.effect.{IO, Resource}
import com.holodome.infrastructure.redis.RedisEphemeralDict
import com.holodome.tests.ResourceSuite
import com.holodome.tests.generators.nonEmptyStringGen
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

import scala.concurrent.duration.DurationInt

object RedisSuite extends ResourceSuite {

  private implicit val logger: Logger[IO] = NoOpLogger[IO]

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  private val Expire = 30.seconds

  test("basic redis operations work") { redis =>
    val gen = for {
      a <- nonEmptyStringGen
      b <- nonEmptyStringGen
    } yield a -> b
    forall(gen) { case (key, value) =>
      val dict = RedisEphemeralDict.make[IO](redis, Expire)
      for {
        x <- dict.get(key).value
        _ <- dict.store(key, value)
        y <- dict.get(key).value
        _ <- dict.delete(key)
      } yield expect.all(x.isEmpty, y.fold(false)(v => v == value))
    }
  }
}
