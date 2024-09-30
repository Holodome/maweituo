package maweituo.tests.ads

import maweituo.domain.ads.*
import maweituo.domain.ads.services.AdService
import maweituo.domain.errors.{AdModificationForbidden, InvalidAdId, InvalidUserId}
import maweituo.domain.services.*
import maweituo.domain.users.services.*
import maweituo.interpreters.*
import maweituo.interpreters.ads.AdServiceInterpreter
import maweituo.interpreters.users.UserServiceInterpreter
import maweituo.tests.generators.*
import maweituo.tests.repos.*
import maweituo.tests.repos.inmemory.*
import maweituo.tests.services.makeIAMService
import maweituo.tests.services.stubs.TelemetryServiceStub

import cats.data.NonEmptyList
import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object Adadsuite extends SimpleIOSuite with Checkers:

  given Logger[IO]           = NoOpLogger[IO]
  given TelemetryService[IO] = new TelemetryServiceStub[IO]

  private def makeTestUserAds: (UserService[IO], AdService[IO]) =
    val userRepo         = InMemoryRepositoryFactory.users
    val adRepo           = InMemoryRepositoryFactory.ads
    given IAMService[IO] = makeIAMService(adRepo)
    val users            = UserServiceInterpreter.make(userRepo)
    val ads              = AdServiceInterpreter.make(adRepo, RepositoryStubFactory.feed)
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
    val (users, ads) = makeTestUserAds
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

  test("get invalid") {
    val (users, ads) = makeTestUserAds
    forall(userIdGen) { id =>
      for
        x <- users.get(id).attempt
      yield expect.same(Left(InvalidUserId(id)), x)
    }
  }

  test("delete works") {
    val (users, ads) = makeTestUserAds
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

  test("delete by other user is blocked") {
    val (users, ads) = makeTestUserAds
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
