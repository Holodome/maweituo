package maweituo
package tests
package resources

import maweituo.tests.containers.makeRedisResource

import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.log4cats.given
import org.typelevel.log4cats.*
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

final case class RedisCon(value: RedisCommands[IO, String, String])

trait RedisContainerResource:
  this: GlobalResource =>

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    given Logger[IO] = NoOpLogger[IO]
    for
      redis <- makeRedisResource[IO].map(RedisCon.apply)
      _     <- global.putR(redis)
    yield ()

extension (global: GlobalRead)
  def redis: Resource[IO, RedisCommands[IO, String, String]] =
    global.getR[RedisCon]().flatMap {
      case Some(value) => Resource.pure(value.value)
      case None =>
        given Logger[IO] = NoOpLogger[IO]
        makeRedisResource[IO]
    }
