package maweituo.it

import scala.concurrent.duration.DurationInt

import cats.effect.*

import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.tests.ResourceSuite
import maweituo.tests.containers.makeRedisResource
import maweituo.tests.generators.nonEmptyStringGen

import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.log4cats.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.*
import weaver.scalacheck.Checkers

object RedisSuite extends ResourceSuite:

  private val Expire = 30.seconds

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    given Logger[IO] = NoOpLogger[IO]
    makeRedisResource[IO]

  private val kwGen =
    for
      a <- nonEmptyStringGen
      b <- nonEmptyStringGen
    yield a -> b

  test("get invalid") { redis =>
    val dict = RedisEphemeralDict.make[IO](redis, Expire)
    forall(nonEmptyStringGen) { key =>
      for
        x <- dict.get(key).value
      yield expect.same(None, x)
    }
  }

  test("create and get") { redis =>
    val dict = RedisEphemeralDict.make[IO](redis, Expire)
    forall(kwGen) { (key, value) =>
      for
        _ <- dict.store(key, value)
        x <- dict.get(key).value
      yield expect.same(Some(value), x)
    }
  }

  test("delete") { redis =>
    val dict = RedisEphemeralDict.make[IO](redis, Expire)
    forall(kwGen) { (key, value) =>
      for
        _ <- dict.store(key, value)
        _ <- dict.delete(key)
        x <- dict.get(key).value
      yield expect.same(None, x)
    }
  }
