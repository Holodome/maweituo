package maweituo
package tests
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

object ChatServiceSuite extends SimpleIOSuite with Checkers with ChatServiceProperties:

  private def makeTestServices =
    given Logger[IO]           = NoOpLogger[IO]
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val chatRepo               = InMemoryRepoFactory.chats
    val userRepo               = InMemoryRepoFactory.users
    val adRepo                 = InMemoryRepoFactory.ads
    given IAMService[IO]       = makeIAMService(adRepo, chatRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val chats                  = ChatServiceInterp.make(chatRepo, adRepo)
    (users, ads, chats)

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestServices)
      }
  }
