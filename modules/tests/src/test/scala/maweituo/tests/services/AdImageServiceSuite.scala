package maweituo
package tests
package services

import maweituo.infrastructure.inmemory.InMemoryObjectStorage
import maweituo.tests.properties.services.AdImageServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.LoggerFactory

object ImageServiceSuite extends MaweituoSimpleSuite with AdImageServiceProperties:

  private def makeTestServices(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
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
      unitTest(name) {
        fn.tupled(makeTestServices)
      }
  }
