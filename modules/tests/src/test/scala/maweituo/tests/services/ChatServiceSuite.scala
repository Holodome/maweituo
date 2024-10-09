package maweituo.tests.services

import cats.effect.IO

import maweituo.domain.services.*
import maweituo.logic.interp.ads.{AdServiceInterp, ChatServiceInterp}
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

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
