package com.holodome.tests.services

import cats.effect.IO
import com.holodome.domain.repositories.*
import com.holodome.domain.services.*
import com.holodome.infrastructure.inmemory.InMemoryObjectStorage
import com.holodome.interpreters.*
import com.holodome.tests.generators.{ createAdRequestGen, imageContentsGen, registerGen }
import com.holodome.tests.repositories.*
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ImageServiceSuite extends SimpleIOSuite with Checkers with MockitoSugar with MockitoCats:
  given Logger[IO] = NoOpLogger[IO]

  private def makeIam(
      ad: AdvertisementRepository[IO],
      images: AdImageRepository[IO]
  ): IAMService[IO] =
    IAMServiceInterpreter.make(ad, mock[ChatRepository[IO]], images)

  test("create works") {
    val gen =
      for
        reg <- registerGen
        ad  <- createAdRequestGen
        img <- imageContentsGen[IO]
      yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      val userRepo  = new InMemoryUserRepository[IO]
      val adRepo    = new InMemoryAdRepository[IO]
      val imageRepo = new InMemoryAdImageRepository[IO]
      val os        = new InMemoryObjectStorage[IO]
      val iam       = makeIam(adRepo, imageRepo)
      val users     = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val ads =
        AdServiceInterpreter.make[IO](
          adRepo,
          mock[TagRepository[IO]],
          new FeedRepositoryStub,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      val imgs = AdImageServiceInterpreter.make[IO](imageRepo, adRepo, os, iam)
      for
        u    <- users.create(reg)
        a    <- ads.create(u, createAd)
        i    <- imgs.upload(u, a, imgCont)
        data <- imgs.get(i)
        d1   <- data.data.compile.toVector
        d2   <- imgCont.data.compile.toVector
      yield expect.all(d1 == d2)
    }
  }
