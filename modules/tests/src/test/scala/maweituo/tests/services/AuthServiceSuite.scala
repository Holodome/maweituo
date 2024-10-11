package maweituo
package tests
package services

import maweituo.domain.all.*
import maweituo.infrastructure.EphemeralDict
import maweituo.infrastructure.inmemory.InMemoryEphemeralDict
import maweituo.logic.auth.JwtTokens
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.AuthServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory

import dev.profunktor.auth.jwt.JwtToken
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

object AuthServiceSuite extends SimpleIOSuite with Checkers with AuthServiceProperties:

  private def jwtDict: EphemeralDict[IO, JwtToken, UserId]         = InMemoryEphemeralDict.make
  private def authedUsersDict: EphemeralDict[IO, UserId, JwtToken] = InMemoryEphemeralDict.make

  private def makeTestUsersAuth(tokens: JwtTokens[IO]) =
    given Logger[IO]     = NoOpLogger[IO]
    val userRepo         = InMemoryRepoFactory.users
    val adRepo           = InMemoryRepoFactory.ads
    given IAMService[IO] = makeIAMService(adRepo)
    val users            = UserServiceInterp.make(userRepo)
    val auth             = AuthServiceInterp.make(userRepo, authedUsersDict, jwtDict, tokens)
    (users, auth)

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp(makeTestUsersAuth)
      }
  }
