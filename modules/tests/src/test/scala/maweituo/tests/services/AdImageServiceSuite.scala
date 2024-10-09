package maweituo.tests.services
import cats.effect.IO

import maweituo.domain.services.*
import maweituo.infrastructure.inmemory.InMemoryObjectStorage
import maweituo.logic.interp.ads.{AdImageServiceInterp, AdServiceInterp}
import maweituo.logic.interp.users.UserServiceInterp
import maweituo.tests.properties.services.AdImageServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ImageServiceSuite extends SimpleIOSuite with Checkers with AdImageServiceProperties:

  private def makeTestServices =
    given Logger[IO]           = NoOpLogger[IO]
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val imageRepo              = InMemoryRepoFactory.images
    val userRepo               = InMemoryRepoFactory.users
    val adRepo                 = InMemoryRepoFactory.ads
    val os                     = new InMemoryObjectStorage
    given IAMService[IO]       = makeIAMService(adRepo, imageRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val images                 = AdImageServiceInterp.make(imageRepo, adRepo, os)
    (users, ads, images)

  properties.foreach {
    case Property(name, fn) =>
      test(name) {
        fn.tupled(makeTestServices)
      }
  }
