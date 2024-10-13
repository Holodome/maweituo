package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.properties.services.AdServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class AdServiceITSuite(global: GlobalRead) extends ResourceSuite with AdServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def testServices(xa: Transactor[IO])(using LoggerFactory[IO]) =
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
    val adRepo                 = PostgresAdRepo.make[IO](xa)
    val userRepo               = PostgresUserRepo.make[IO](xa)
    given IAMService[IO]       = makeIAMService(adRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    (users, ads)

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (postgres, log) =>
        given LoggerFactory[IO] = WeaverLogAdapterFactory[IO](log)
        fn.tupled(testServices(postgres))
      }
  }
