package maweituo
package tests
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

object ChatServiceSuite extends MaweituoSimpleSuite with ChatServiceProperties:

  private def makeTestServices(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
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
      unitTest(name) {
        exp.tupled(makeTestServices)
      }
  }
