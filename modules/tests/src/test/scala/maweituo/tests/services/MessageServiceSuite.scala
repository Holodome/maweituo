package maweituo.tests.services
import cats.MonadThrow
import cats.effect.IO

import maweituo.domain.services.*
import maweituo.interp.*
import maweituo.interp.ads.{AdServiceInterp, ChatServiceInterp, MessageServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.properties.services.MessageServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object MessageServiceSuite extends SimpleIOSuite with Checkers with MessageServiceProperties:

  private def makeTestServices =
    given Logger[IO]           = NoOpLogger[IO]
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val msgRepo                = InMemoryRepoFactory.msgs
    val userRepo               = InMemoryRepoFactory.users
    val adRepo                 = InMemoryRepoFactory.ads
    val chatRepo               = InMemoryRepoFactory.chats
    given iam: IAMService[IO]  = makeIAMService(adRepo, chatRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val chats                  = ChatServiceInterp.make(chatRepo, adRepo)
    val msgs                   = MessageServiceInterp.make(msgRepo)(using MonadThrow[IO], timeSource, iam)
    (users, ads, chats, msgs)

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestServices)
      }
  }
