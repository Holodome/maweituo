package maweituo
package tests
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

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
