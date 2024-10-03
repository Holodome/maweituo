package maweituo.it.resources

import cats.effect.{IO, Resource}

import maweituo.tests.containers.makeRedisResource

import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.log4cats.given
import org.typelevel.log4cats.*
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.{GlobalRead, GlobalResource, GlobalWrite}

object RedisContainerResource extends GlobalResource:

  final case class RedisCon(value: RedisCommands[IO, String, String])

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    given Logger[IO] = NoOpLogger[IO]
    for
      pg <- makeRedisResource[IO].map(RedisCon.apply)
      _  <- global.putR(pg)
    yield ()

extension (global: GlobalRead)
  def redis: Resource[IO, RedisCommands[IO, String, String]] =
    global.getR[RedisContainerResource.RedisCon]().flatMap {
      case Some(value) => Resource.pure(value.value)
      case None =>
        given Logger[IO] = NoOpLogger[IO]
        makeRedisResource[IO]
    }
