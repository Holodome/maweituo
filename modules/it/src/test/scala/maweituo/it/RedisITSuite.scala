package maweituo.it

import scala.concurrent.duration.DurationInt

import cats.effect.*

import maweituo.infrastructure.EphemeralDict
import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.nonEmptyStringGen
import maweituo.tests.resources.*

import dev.profunktor.redis4cats.RedisCommands
import weaver.*
import weaver.scalacheck.Checkers

class RedisSuite(global: GlobalRead) extends ResourceSuite:

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] = global.redis

  private val kwGen =
    for
      a <- nonEmptyStringGen
      b <- nonEmptyStringGen
    yield a -> b

  private val Expire = 30.seconds

  private def redisTest(name: String)(fn: EphemeralDict[IO, String, String] => F[Expectations]) =
    test(name) { redis =>
      fn(RedisEphemeralDict.make[IO](redis, Expire))
    }

  redisTest("get invalid") { dict =>
    forall(nonEmptyStringGen) { key =>
      for
        x <- dict.get(key).value
      yield expect.same(None, x)
    }
  }

  redisTest("create and get") { dict =>
    forall(kwGen) { (key, value) =>
      for
        _ <- dict.store(key, value)
        x <- dict.get(key).value
      yield expect.same(Some(value), x)
    }
  }

  redisTest("delete") { dict =>
    forall(kwGen) { (key, value) =>
      for
        _ <- dict.store(key, value)
        _ <- dict.delete(key)
        x <- dict.get(key).value
      yield expect.same(None, x)
    }
  }
