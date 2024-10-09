package maweituo.it.services

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*

import maweituo.logic.auth.JwtTokens
import maweituo.domain.services.IAMService
import maweituo.infrastructure.redis.RedisEphemeralDict
import maweituo.logic.interp.AuthServiceInterp
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.modules.Infrastructure
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.AuthServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class AuthServiceITSuite(global: GlobalRead) extends ResourceSuite with AuthServiceProperties:

  type Res = (Transactor[IO], RedisCommands[IO, String, String])

  override def sharedResource: Resource[IO, Res] = (global.postgres, global.redis).tupled

  private val expire = 30.seconds

  private def makeTestServices(xa: Transactor[IO], redis: RedisCommands[IO, String, String])(using Logger[IO]) =
    (jwt: JwtTokens[IO]) =>
      given IAMService[IO] = makeIAMService
      val repo             = PostgresUserRepo.make[IO](xa)
      val jwtDict          = Infrastructure.ephemeralDictToJwt(RedisEphemeralDict.make(redis, expire))
      val authedUsersDict  = Infrastructure.ephemeralDictToUsers(RedisEphemeralDict.make(redis, expire))
      (UserServiceInterp.make(repo), AuthServiceInterp.make(repo, jwtDict, authedUsersDict, jwt))

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (res, log) =>
        given Logger[IO] = WeaverLogAdapter(log)
        fn(makeTestServices.tupled(res))
      }
  }
