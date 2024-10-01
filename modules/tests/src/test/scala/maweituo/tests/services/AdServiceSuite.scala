package maweituo.tests.ads

import scala.util.control.NoStackTrace

import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO

import maweituo.domain.ads.*
import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.ads.services.AdService
import maweituo.domain.errors.{AdModificationForbidden, InvalidAdId}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.interp.*
import maweituo.interp.ads.AdServiceInterp
import maweituo.interp.users.UserServiceInterp
import maweituo.tests.generators.*
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AdServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestUserAds(adRepo: AdRepo[IO] = InMemoryRepoFactory.ads)
      : (UserService[IO], AdService[IO]) =
    val userRepo         = InMemoryRepoFactory.users
    given IAMService[IO] = makeIAMService(adRepo)
    val users            = UserServiceInterp.make(userRepo)
    val ads              = AdServiceInterp.make(adRepo, RepoStubFactory.feed)
    (users, ads)

  private val regAdGen =
    for
      reg <- registerGen
      ad  <- createAdRequestGen
    yield reg -> ad

  private val regAdTagGen =
    for
      reg <- registerGen
      ad  <- createAdRequestGen
      tag <- adTagGen
    yield (reg, ad, tag)

  test("create works") {
    val (users, ads) = makeTestUserAds()
    forall(regAdGen) { case (reg, createAd) =>
      for
        userId <- users.create(reg)
        adId   <- ads.create(userId, createAd)
        ad     <- ads.get(adId)
      yield NonEmptyList.of(
        expect.same(ad.title, createAd.title),
        expect.same(ad.id, adId),
        expect.same(ad.authorId, userId)
      ).reduce
    }
  }

  test("create internal error") {
    case class TestError() extends NoStackTrace
    class TestAds extends InMemoryAdRepo[IO]:
      override def create(ad: Advertisement): F[Unit] = IO.raiseError(TestError())
    val (users, ads) = makeTestUserAds(new TestAds)
    forall(regAdGen) { case (reg, createAd) =>
      for
        userId <- users.create(reg)
        x      <- ads.create(userId, createAd).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("get invalid") {
    val (users, ads) = makeTestUserAds()
    forall(adIdGen) { id =>
      for
        x <- ads.get(id).attempt
      yield expect.same(Left(InvalidAdId(id)), x)
    }
  }

  test("get internal error") {
    case class TestError() extends NoStackTrace
    class TestAds extends InMemoryAdRepo[IO]:
      override def find(id: AdId): OptionT[IO, Advertisement] = OptionT(IO.raiseError(TestError()))
    val (users, ads) = makeTestUserAds(new TestAds)
    forall(adIdGen) { case id =>
      for
        x <- ads.get(id).attempt
      yield expect.same(Left(TestError()), x)
    }
  }

  test("delete works") {
    val (users, ads) = makeTestUserAds()
    forall(regAdGen) { case (reg, createAd) =>
      for
        userId <- users.create(reg)
        adId   <- ads.create(userId, createAd)
        _      <- ads.get(adId)
        _      <- ads.delete(adId, userId)
        x      <- ads.get(adId).attempt
      yield expect.same(Left(InvalidAdId(adId)), x)
    }
  }

  test("delete internal error") {
    case class TestError() extends NoStackTrace
    class TestAds extends InMemoryAdRepo[IO]:
      override def delete(id: AdId) = IO.raiseError(TestError())
    val (users, ads) = makeTestUserAds(new TestAds)
    val gen =
      for
        u <- userIdGen
        a <- adIdGen
      yield u -> a
    forall(gen) { case (u, a) =>
      for
        x <- ads.delete(a, u).attempt
      yield expect.same(Left(InvalidAdId(a)), x)
    }
  }

  test("delete by other user is blocked") {
    val (users, ads) = makeTestUserAds()
    val gen =
      for
        reg  <- registerGen
        ad   <- createAdRequestGen
        reg1 <- registerGen
      yield (reg, ad, reg1)
    forall(gen) { case (reg, createAd, otherReg) =>
      for
        userId  <- users.create(reg)
        otherId <- users.create(otherReg)
        adId    <- ads.create(userId, createAd)
        _       <- ads.get(adId)
        x       <- ads.delete(adId, otherId).attempt
        a       <- ads.get(adId)
      yield expect.same(Left(AdModificationForbidden(adId, otherId)), x) and expect.same(a.title, createAd.title)
    }
  }
