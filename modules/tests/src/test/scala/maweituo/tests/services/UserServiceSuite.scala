package maweituo
package tests
package services

import maweituo.tests.properties.services.UserServiceProperties
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService

import org.typelevel.log4cats.noop.NoOpFactory

object UserServiceSuite extends MaweituoSimpleSuite with UserServiceProperties:

  private def makeTestUsers(using LoggerFactory[IO]) =
    given IAMService[IO] = makeIAMService
    val repo             = InMemoryRepoFactory.users
    UserServiceInterp.make(repo)

  properties.foreach {
    case Property(name, fn) =>
      unitTest(name) {
        fn(makeTestUsers)
      }
  }
