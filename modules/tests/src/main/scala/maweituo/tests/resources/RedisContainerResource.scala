package maweituo
package tests
package resources

import maweituo.tests.containers.{PartiallyAppliedRedis, makeRedisResource}

import weaver.{GlobalRead, GlobalResource, GlobalWrite}

trait RedisContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for
      redis <- makeRedisResource
      _     <- global.putR(redis)
    yield ()

extension (global: GlobalRead)
  def redis: Resource[IO, PartiallyAppliedRedis] =
    global.getR[PartiallyAppliedRedis]().flatMap {
      case Some(value) => Resource.pure(value)
      case None        => makeRedisResource
    }
