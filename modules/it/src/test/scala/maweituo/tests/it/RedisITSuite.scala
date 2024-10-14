package maweituo
package tests
package it

import scala.concurrent.duration.DurationInt

import maweituo.infrastructure.EphemeralDict
import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.tests.containers.PartiallyAppliedRedis
import maweituo.tests.resources.*

import weaver.*
import weaver.scalacheck.Checkers

class RedisSuite(global: GlobalRead) extends ResourceSuite:

  type Res = PartiallyAppliedRedis

  override def sharedResource: Resource[IO, Res] = global.redis

  private val kwGen =
    for
      a <- nonEmptyStringGen
      b <- nonEmptyStringGen
    yield a -> b

  private val Expire = 30.seconds

  private def redisTest(name: String)(fn: EphemeralDict[IO, String, String] => F[Expectations]) =
    itTest(name) { redis0 =>
      redis0().use { redis =>
        fn(RedisEphemeralDict.make[IO](redis, Expire))
      }
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
