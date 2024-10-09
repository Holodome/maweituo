package maweituo.it.services

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*

import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.infrastructure.minio.{MinioConnection, MinioObjectStorage}
import maweituo.interp.ads.{AdImageServiceInterp, AdServiceInterp}
import maweituo.interp.users.UserServiceInterp
import maweituo.postgres.repos.ads.{PostgresAdImageRepo, PostgresAdRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.AdImageServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class AdImageServiceITSuite(global: GlobalRead) extends ResourceSuite with AdImageServiceProperties:

  type Res = (Transactor[IO], MinioConnection)

  override def sharedResource: Resource[IO, Res] = (global.postgres, global.minio).tupled

  private def testServices(xa: Transactor[IO], minio: MinioConnection)(using Logger[IO]) =
    given TelemetryService[IO] = new TelemetryServiceStub[IO]
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
        given Logger[IO] = WeaverLogAdapter(log)
        testServices.tupled(res).flatMap(fn.tupled)
      }
  }
