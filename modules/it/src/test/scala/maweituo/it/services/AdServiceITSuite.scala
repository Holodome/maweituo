package maweituo.it.services

import cats.effect.IO
import cats.effect.kernel.Resource

import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.interp.ads.AdServiceInterp
import maweituo.interp.users.UserServiceInterp
import maweituo.postgres.ads.repos.PostgresAdRepo
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.properties.services.AdServiceProperties
import maweituo.tests.resources.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead

class AdServiceITSuite(global: GlobalRead) extends ResourceSuite with AdServiceProperties:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] = global.postgres

  private def testServices(xa: Transactor[IO])(using Logger[IO]) =
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
        given Logger[IO] = new WeaverLogAdapter[IO](log)
        fn.tupled(testServices(postgres))
      }
  }
