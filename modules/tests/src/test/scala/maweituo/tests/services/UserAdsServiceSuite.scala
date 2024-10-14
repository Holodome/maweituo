package maweituo
package tests
package servicesimport

import maweituo.tests.properties.services.UserAdsServiceProperties
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

object UserAdServiceSuite extends MaweituoSimpleSuite with UserAdsServiceProperties:

  given TelemetryService[IO] = TelemetryServiceStub[IO]

  private def makeTestServices(using LoggerFactory[IO]) =
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
      unitTest(name) {
        exp.tupled(makeTestServices)
      }
  }
