package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.infrastructure.minio.{MinioConnection, MinioObjectStorage}
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.properties.services.AdImageServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class AdImageServiceITSuite(global: GlobalRead) extends ResourceSuite with AdImageServiceProperties:

  type Res = (Transactor[IO], MinioConnection)

  override def sharedResource: Resource[IO, Res] = (global.postgres, global.minio).tupled

  private def testServices(xa: Transactor[IO], minio: MinioConnection)(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
    val imageRepo              = PostgresAdImageRepo.make(xa)
    val adRepo                 = PostgresAdRepo.make(xa)
    val userRepo               = PostgresUserRepo.make(xa)
    for
      os <- MinioObjectStorage.make(minio, "maweituo")
      given IAMService[IO] = makeIAMService(adRepo, imageRepo)
      users                = UserServiceInterp.make(userRepo)
      ads                  = AdServiceInterp.make(adRepo)
      images               = AdImageServiceInterp.make(imageRepo, adRepo, os)
    yield (users, ads, images)

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (res, log) =>
        given LoggerFactory[IO] = WeaverLogAdapterFactory[IO](log)
        testServices.tupled(res).flatMap(fn.tupled)
      }
  }
