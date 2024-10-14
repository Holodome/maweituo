package maweituo
package tests
package it
package services

import maweituo.domain.all.*
import maweituo.infrastructure.ObjectStorage
import maweituo.logic.interp.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.containers.{PartiallyAppliedMinio, PartiallyAppliedPostgres}
import maweituo.tests.properties.services.AdImageServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import doobie.util.transactor.Transactor
import weaver.GlobalRead

class AdImageServiceITSuite(global: GlobalRead) extends ResourceSuite with AdImageServiceProperties:

  type Res = (PartiallyAppliedPostgres, PartiallyAppliedMinio)

  override def sharedResource: Resource[IO, Res] = (global.postgres, global.minio).tupled

  private def testServices(xa: Transactor[IO], minio: ObjectStorage[IO])(using LoggerFactory[IO]) =
    given TelemetryService[IO] = TelemetryServiceStub[IO]
    val imageRepo              = PostgresAdImageRepo.make(xa)
    val adRepo                 = PostgresAdRepo.make(xa)
    val userRepo               = PostgresUserRepo.make(xa)
    given IAMService[IO]       = makeIAMService(adRepo, imageRepo)
    val users                  = UserServiceInterp.make(userRepo)
    val ads                    = AdServiceInterp.make(adRepo)
    val images                 = AdImageServiceInterp.make(imageRepo, adRepo, minio)
    (users, ads, images)

  properties.foreach {
    case Property(name, fn) =>
      itTest(name) { (pg0, minio0) =>
        Resource.both(pg0(), minio0()).use { (postgres, minio) =>
          fn.tupled(testServices(postgres, minio))
        }
      }
  }
