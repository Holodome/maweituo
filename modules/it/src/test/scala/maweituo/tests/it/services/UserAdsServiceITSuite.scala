package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.tests.properties.services.UserAdsServiceProperties
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class UserAdsServiceITSuite(global: GlobalRead) extends PostgresITSuite(global) with UserAdsServiceProperties:

  private def testServices(xa: Transactor[IO])(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
    val adRepo                 = PostgresAdRepo.make[IO](xa)
    val userRepo               = PostgresUserRepo.make[IO](xa)
    given IAMService[IO]       = makeIAMService(adRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val userAds                = UserAdsServiceInterp.make(adRepo)
    (users, ads, userAds)

  properties.foreach {
    case Property(name, fn) =>
      pgTest(name) { postgres =>
        fn.tupled(testServices(postgres))
      }
  }
