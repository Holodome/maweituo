package com.holodome.tests.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.errors.{InvalidAdId, NotAnAuthor}
import com.holodome.domain.repositories._
import com.holodome.domain.services._
import com.holodome.interpreters._
import com.holodome.tests.generators._
import com.holodome.tests.repositories._
import org.mockito.MockitoSugar.mock
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AdServiceSuite extends SimpleIOSuite with Checkers {
  implicit val logger: Logger[IO] = NoOpLogger[IO]

  private def makeIam(ad: AdvertisementRepository[IO]): IAMService[IO] =
    IAMServiceInterpreter.make(ad, mock[ChatRepository[IO]], mock[AdImageRepository[IO]])

  private val feedRepository: FeedRepository[IO] = new FeedRepositoryStub

  test("create works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
    } yield reg -> ad
    forall(gen) { case (reg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val serv = AdServiceInterpreter
        .make[IO](
          adRepo,
          mock[TagRepository[IO]],
          feedRepository,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      for {
        userId <- users.create(reg)
        adId   <- serv.create(userId, createAd)
        ad     <- serv.get(adId)
      } yield expect.all(
        ad.title === createAd.title,
        ad.id === adId,
        ad.images.isEmpty,
        ad.tags.isEmpty,
        ad.chats.isEmpty,
        ad.authorId === userId
      )
    }
  }

  test("delete works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
    } yield reg -> ad
    forall(gen) { case (reg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val serv = AdServiceInterpreter
        .make[IO](
          adRepo,
          mock[TagRepository[IO]],
          feedRepository,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      for {
        userId <- users.create(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.get(adId)
        _      <- serv.delete(adId, userId)
        x      <- serv.get(adId).map(Some(_)).recoverWith { case InvalidAdId(_) => None.pure[IO] }
      } yield expect.all(x.isEmpty)
    }
  }

  test("delete by other user is blocked") {
    val gen = for {
      reg      <- registerGen
      ad       <- createAdRequestGen
      otherReg <- registerGen
    } yield (reg, ad, otherReg)
    forall(gen) { case (reg, createAd, otherReg) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val serv = AdServiceInterpreter.make[IO](
        adRepo,
        mock[TagRepository[IO]],
        feedRepository,
        new UserAdsRepositoryStub,
        iam,
        new TelemetryServiceStub[IO]
      )
      for {
        userId  <- users.create(reg)
        otherId <- users.create(otherReg)
        adId    <- serv.create(userId, createAd)
        _       <- serv.get(adId)
        x <- serv.delete(adId, otherId).map(Some(_)).recoverWith { case NotAnAuthor(_, _) =>
          None.pure[IO]
        }
        _ <- serv.get(adId)
      } yield expect.all(x.isEmpty)
    }
  }

  test("add tag works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
      tag <- adTagGen
    } yield (reg, ad, tag)
    forall(gen) { case (reg, createAd, tag) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val tagRepo  = new InMemoryTagRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val serv = AdServiceInterpreter
        .make[IO](
          adRepo,
          tagRepo,
          feedRepository,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      for {
        userId <- users.create(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addTag(adId, tag, userId)
        ad     <- serv.get(adId)
      } yield expect.all(ad.tags === Set(tag))
    }
  }

  test("remove tag works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
      tag <- adTagGen
    } yield (reg, ad, tag)
    forall(gen) { case (reg, createAd, tag) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val tagRepo  = new InMemoryTagRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserServiceInterpreter.make[IO](userRepo, new UserAdsRepositoryStub, iam)
      val serv = AdServiceInterpreter
        .make[IO](
          adRepo,
          tagRepo,
          feedRepository,
          new UserAdsRepositoryStub,
          iam,
          new TelemetryServiceStub[IO]
        )
      for {
        userId <- users.create(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addTag(adId, tag, userId)
        _      <- serv.removeTag(adId, tag, userId)
        ad     <- serv.get(adId)
      } yield expect.all(ad.tags === Set())
    }
  }
}
