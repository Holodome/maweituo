package maweituo
package tests
package it
package services

import scala.concurrent.duration.DurationInt

import maweituo.domain.all.*
import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.logic.auth.JwtTokens
import maweituo.logic.interp.all.*
import maweituo.modules.Infrastructure
import maweituo.postgres.repos.all.*
import maweituo.tests.containers.{PartiallyAppliedPostgres, PartiallyAppliedRedis}
import maweituo.tests.properties.services.AuthServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService

import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import weaver.GlobalRead

class AuthServiceITSuite(global: GlobalRead) extends ResourceSuite with AuthServiceProperties:

  type Res = (PartiallyAppliedPostgres, PartiallyAppliedRedis)

  override def sharedResource: Resource[IO, Res] = (global.postgres, global.redis).tupled

  private val expire = 30.seconds

  private def makeTestServices(xa: Transactor[IO], redis: RedisCommands[IO, String, String])(using LoggerFactory[IO]) =
    (jwt: JwtTokens[IO]) =>
      given IAMService[IO] = makeIAMService
      val repo             = PostgresUserRepo.make[IO](xa)
      val jwtDict          = Infrastructure.ephemeralDictToJwt(RedisEphemeralDict.make(redis, expire))
      val authedUsersDict  = Infrastructure.ephemeralDictToUsers(RedisEphemeralDict.make(redis, expire))
      (UserServiceInterp.make(repo), AuthServiceInterp.make(repo, jwtDict, authedUsersDict, jwt))

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (pg0, redis0) =>
        Resource.both(pg0(), redis0()).use { (postgres, redis) =>
          fn(makeTestServices(postgres, redis))
        }
      }
  }
