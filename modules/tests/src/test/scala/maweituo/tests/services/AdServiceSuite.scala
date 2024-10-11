package maweituo
package tests
package ads

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.AdServiceProperties
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

object AdServiceSuite extends SimpleIOSuite with Checkers with AdServiceProperties:

  private def makeTestUserAds =
    given Logger[IO]           = NoOpLogger[IO]
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val adRepo                 = InMemoryRepoFactory.ads
    val userRepo               = InMemoryRepoFactory.users
    given IAMService[IO]       = makeIAMService(adRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    (users, ads)

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestUserAds)
      }
  }
