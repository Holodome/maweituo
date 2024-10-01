package maweituo.it.postgres.repos

import scala.concurrent.duration.DurationInt

import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.tests.containers.makeRedisResource
import maweituo.tests.generators.nonEmptyStringGen

import cats.effect.*
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.log4cats.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.*
import weaver.scalacheck.Checkers

// object PostgresUserRepoITSuite extends IOSuite with Checkers:
  
