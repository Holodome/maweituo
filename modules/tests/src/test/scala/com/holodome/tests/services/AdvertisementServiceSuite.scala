package com.holodome.tests.ads

import com.holodome.domain.ads.*
import com.holodome.domain.errors.{ InvalidAdId, NotAnAuthor }
import com.holodome.domain.services.*
import com.holodome.interpreters.*
import com.holodome.tests.generators.*
import com.holodome.tests.repositories.*
import com.holodome.tests.repositories.inmemory.*
import com.holodome.tests.repositories.stubs.RepositoryStubFactory
import com.holodome.tests.services.stubs.TelemetryServiceStub

import cats.data.NonEmptyList
import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object Adadsuite extends SimpleIOSuite with Checkers:
  given Logger[IO] = NoOpLogger[IO]

  private def makeTestUserAds: (UserService[F], AdService[F]) =
    val userRepo = InMemoryRepositoryFactory.users[IO]
    val adRepo   = InMemoryRepositoryFactory.ads[IO]
    val iam      = IAMServiceInterpreter.make(adRepo, RepositoryStubFactory.chats, RepositoryStubFactory.images)
    val users    = UserServiceInterpreter.make[IO](userRepo, adRepo, iam)
    val ads = AdServiceInterpreter
      .make[IO](
        adRepo,
        RepositoryStubFactory.tags,
        RepositoryStubFactory.feed,
        iam,
        new TelemetryServiceStub[IO]
      )
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
      yield expect.same(Left(NotAnAuthor(adId, otherId)), x) and expect.same(a.title, createAd.title)
    }
  }
