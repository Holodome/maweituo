package maweituo.tests.servicesimport

import cats.effect.IO

import maweituo.domain.ads.services.AdService
import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.domain.users.services.{UserAdsService, UserService}
import maweituo.interp.*
import maweituo.interp.ads.AdServiceInterp
import maweituo.interp.users.{UserAdsServiceInterp, UserServiceInterp}
import maweituo.tests.properties.services.UserAdsServiceProperties
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepoFactory
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserAdServiceSuite extends SimpleIOSuite with Checkers with UserAdsServiceProperties:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices: (UserService[IO], AdService[IO], UserAdsService[IO]) =
    val ads              = InMemoryRepoFactory.ads
    val users            = InMemoryRepoFactory.users
    given IAMService[IO] = makeIAMService(ads)
    (
      UserServiceInterp.make(users),
      AdServiceInterp.make(ads, RepoStubFactory.feed),
      UserAdsServiceInterp.make(ads)
    )

  properties.foreach {
    case Property(name, exp) =>
      test(name) {
        exp.tupled(makeTestServices)
      }
  }
