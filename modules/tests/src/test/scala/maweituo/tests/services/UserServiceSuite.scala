package maweituo.tests.services
import cats.effect.IO

import maweituo.domain.services.IAMService
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserServiceSuite extends SimpleIOSuite with Checkers with UserServiceProperties:

  private def makeTestUsers =
    given Logger[IO]     = NoOpLogger[IO]
    given IAMService[IO] = makeIAMService
    val repo             = InMemoryRepoFactory.users
    UserServiceInterp.make(repo)

  properties.foreach {
    case Property(name, fn) =>
      test(name) {
        fn(makeTestUsers)
      }
  }
