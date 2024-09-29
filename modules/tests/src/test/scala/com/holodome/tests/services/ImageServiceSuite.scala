package com.holodome.tests.services

import com.holodome.domain.services.*
import com.holodome.infrastructure.inmemory.InMemoryObjectStorage
import com.holodome.interpreters.*
import com.holodome.tests.generators.{ createAdRequestGen, imageContentsGen, registerGen }
import com.holodome.tests.repositories.*
import com.holodome.tests.repositories.inmemory.*
import com.holodome.tests.repositories.stubs.RepositoryStubFactory
import com.holodome.tests.services.stubs.TelemetryServiceStub

import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ImageServiceSuite extends SimpleIOSuite with Checkers:

  given Logger[IO] = NoOpLogger[IO]

  private def makeTestServices: (UserService[IO], AdService[IO], AdImageService[IO]) =
    val userRepo  = InMemoryRepositoryFactory.users
    val adRepo    = InMemoryRepositoryFactory.ads
    val iam       = IAMServiceInterpreter.make(adRepo, RepositoryStubFactory.chats, RepositoryStubFactory.images)
    val imageRepo = InMemoryRepositoryFactory.images
    val os        = new InMemoryObjectStorage[IO]
    val users     = UserServiceInterpreter.make[IO](userRepo, adRepo, iam)
    val ads = AdServiceInterpreter
      .make[IO](
        adRepo,
        RepositoryStubFactory.tags,
        RepositoryStubFactory.feed,
        iam,
        new TelemetryServiceStub[IO]
      )
    val images = AdImageServiceInterpreter.make[IO](imageRepo, adRepo, os, iam)
    (users, ads, images)

  test("create works") {
    val (users, ads, images) = makeTestServices
    val gen =
      for
        reg <- registerGen
        ad  <- createAdRequestGen
        img <- imageContentsGen[IO]
      yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      for
        u    <- users.create(reg)
        a    <- ads.create(u, createAd)
        i    <- images.upload(u, a, imgCont)
        data <- images.get(i)
        d1   <- data.data.compile.toVector
        d2   <- imgCont.data.compile.toVector
      yield expect.same(d1, d2)
    }
  }
