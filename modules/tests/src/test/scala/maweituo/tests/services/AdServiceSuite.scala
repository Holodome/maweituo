package maweituo
package tests
package ads

import maweituo.tests.properties.services.AdServiceProperties
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

object AdServiceSuite extends MaweituoSimpleSuite with AdServiceProperties:

  private def makeTestUserAds(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
    val adRepo                 = InMemoryRepoFactory.ads
    val userRepo               = InMemoryRepoFactory.users
    given IAMService[IO]       = makeIAMService(adRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    (users, ads)

  properties.foreach {
    case Property(name, exp) =>
      unitTest(name) {
        exp.tupled(makeTestUserAds)
      }
  }
