package maweituo.tests.services
import scala.util.control.NoStackTrace

import cats.effect.IO

import maweituo.domain.ads.AdId
import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.services.AdService
import maweituo.domain.services.{IAMService, TelemetryService}
import maweituo.domain.users.UserId
import maweituo.domain.users.services.{UserAdsService, UserService}
import maweituo.interp.*
import maweituo.interp.ads.AdServiceInterp
import maweituo.interp.users.{UserAdsServiceInterp, UserServiceInterp}
import maweituo.tests.generators.{createAdRequestGen, registerGen, userIdGen}
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.{InMemoryAdRepo, InMemoryRepoFactory}
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UserAdServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestServices(ads: AdRepo[IO] = InMemoryRepoFactory.ads)
      : (UserService[IO], UserAdsService[IO], AdService[IO]) =
    val users            = InMemoryRepoFactory.users
    given IAMService[IO] = makeIAMService(ads)
    (
      UserServiceInterp.make(users),
      UserAdsServiceInterp.make(ads),
      AdServiceInterp.make(ads, RepoStubFactory.feed)
    )

  test("internal error") {
    case class TestError() extends NoStackTrace
    class TestAds extends InMemoryAdRepo[IO]:
      override def findIdsByAuthor(userId: UserId): F[List[AdId]] = IO.raiseError(TestError())
    val (users, userAds, _) = makeTestServices(new TestAds)
    forall(registerGen) { reg =>
      for
        u   <- users.create(reg)
        ads <- userAds.getAds(u).attempt
      yield expect.same(Left(TestError()), ads)
    }
  }

  test("new user has no ads") {
    val (users, userAds, _) = makeTestServices()
    forall(registerGen) { reg =>
      for
        u   <- users.create(reg)
        ads <- userAds.getAds(u)
      yield expect.same(List(), ads)
    }
  }

  test("user has ad after create") {
    val (users, userAds, ads) = makeTestServices()
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
    val (users, userAds, ads) = makeTestServices()
    forall(userIdGen) { id =>
      for
        x <- userAds.getAds(id)
      yield expect.same(List(), x)
    }
  }
