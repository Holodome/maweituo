package maweituo.tests.services
import maweituo.domain.ads.services.AdService
import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.domain.users.UserId
import maweituo.domain.users.services.{UserAdsService, UserService}
import maweituo.interpreters.*
import maweituo.interpreters.ads.AdServiceInterpreter
import maweituo.interpreters.users.{UserAdsServiceInterpreter, UserServiceInterpreter}
import maweituo.tests.generators.{createAdRequestGen, registerGen, userIdGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.InMemoryRepositoryFactory
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserAdServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices: (UserService[IO], UserAdsService[IO], AdService[IO]) =
    val users            = InMemoryRepositoryFactory.users
    val ads              = InMemoryRepositoryFactory.ads
    given IAMService[IO] = makeIAMService(ads)
    (
      UserServiceInterpreter.make(users),
      UserAdsServiceInterpreter.make(ads),
      AdServiceInterpreter.make(ads, RepositoryStubFactory.feed)
    )

  test("new user has no ads") {
    val (users, userAds, _) = makeTestServices
    forall(registerGen) { reg =>
      for
        u   <- users.create(reg)
        ads <- userAds.getAds(u)
      yield expect.same(List(), ads)
    }
  }

  test("user has ad after create") {
    val (users, userAds, ads) = makeTestServices
    val gen =
      for
        r  <- registerGen
        ad <- createAdRequestGen
      yield r -> ad
    forall(gen) { (reg, ad) =>
      for
        u <- users.create(reg)
        a <- ads.create(u, ad)
        x <- userAds.getAds(u)
      yield expect.same(List(a), x)
    }
  }

  test("invalid user has no ads") {
    val (users, userAds, ads) = makeTestServices
    forall(userIdGen) { id =>
      for
        x <- userAds.getAds(id)
      yield expect.same(List(), x)
    }
  }
