package maweituo
package tests
package servicesimport

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.UserAdsServiceProperties
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.noop.NoOpFactory

object UserAdServiceSuite extends SimpleIOSuite with Checkers with UserAdsServiceProperties:

  given LoggerFactory[IO]    = NoOpFactory[IO]
  given TelemetryService[IO] = TelemetryServiceStub[IO]

  private def makeTestServices =
    val ads              = InMemoryRepoFactory.ads
    val users            = InMemoryRepoFactory.users
    given IAMService[IO] = makeIAMService(ads)
    (
      UserServiceInterp.make(users),
      AdServiceInterp.make(ads),
      UserAdsServiceInterp.make(ads)
    )

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestServices)
      }
  }
