package maweituo.tests.services
import cats.effect.IO

import maweituo.domain.ads.services.{AdService, ChatService}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.interp.*
import maweituo.interp.ads.{AdServiceInterp, ChatServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.properties.services.ChatServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.*

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ChatServiceSuite extends SimpleIOSuite with Checkers with ChatServiceProperties:

  private def makeTestServices: (UserService[F], AdService[F], ChatService[F]) =
    given Logger[IO]           = NoOpLogger[IO]
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val chatRepo               = InMemoryRepoFactory.chats
    val telemetry              = new TelemetryServiceStub
    val userRepo               = InMemoryRepoFactory.users
    val adRepo                 = InMemoryRepoFactory.ads
    given IAMService[IO]       = makeIAMService(adRepo, chatRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val feedRepo               = RepoStubFactory.feed
    val ads                    = AdServiceInterp.make(adRepo, feedRepo)
    val chats                  = ChatServiceInterp.make(chatRepo, adRepo)
    (users, ads, chats)

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestServices)
      }
  }
