package com.holodome.services

import cats.effect.IO
import cats.syntax.all._
import com.holodome.domain.ads.{InvalidAdId, NotAnAuthor}
import com.holodome.generators._
import com.holodome.repositories._
import org.mockito.MockitoSugar.mock
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object AdvertisementServiceSuite extends SimpleIOSuite with Checkers {
  private def makeIam(ad: AdvertisementRepository[IO]): IAMService[IO] =
    IAMService.make(ad, mock[ChatRepository[IO]], mock[ImageRepository[IO]])

  test("create works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
    } yield reg -> ad
    forall(gen) { case (reg, createAd) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        ad     <- serv.find(adId)
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
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.find(adId)
        _      <- serv.delete(adId, userId)
        x      <- serv.find(adId).map(Some(_)).recoverWith { case InvalidAdId(_) => None.pure[IO] }
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
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId  <- users.register(reg)
        otherId <- users.register(otherReg)
        adId    <- serv.create(userId, createAd)
        _       <- serv.find(adId)
        x <- serv.delete(adId, otherId).map(Some(_)).recoverWith { case NotAnAuthor() =>
          None.pure[IO]
        }
        _ <- serv.find(adId)
      } yield expect.all(x.isEmpty)
    }
  }

  test("add image works") {
    val gen = for {
      reg     <- registerGen
      ad      <- createAdRequestGen
      imageId <- imageIdGen
    } yield (reg, ad, imageId)
    forall(gen) { case (reg, createAd, imageId) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addImage(adId, imageId, userId)
        ad     <- serv.find(adId)
      } yield expect.all(ad.images === Set(imageId))
    }
  }

  test("delete image works") {
    val gen = for {
      reg     <- registerGen
      ad      <- createAdRequestGen
      imageId <- imageIdGen
    } yield (reg, ad, imageId)
    forall(gen) { case (reg, createAd, imageId) =>
      val userRepo = new InMemoryUserRepository[IO]
      val adRepo   = new InMemoryAdRepository[IO]
      val iam      = makeIam(adRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addImage(adId, imageId, userId)
        _      <- serv.removeImage(adId, imageId, userId)
        ad     <- serv.find(adId)
      } yield expect.all(ad.images === Set())
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
      val iam      = makeIam(adRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addTag(adId, tag, userId)
        ad     <- serv.find(adId)
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
      val iam      = makeIam(adRepo)
      val users    = UserService.make[IO](userRepo, iam)
      val serv     = AdvertisementService.make[IO](adRepo, iam)
      for {
        userId <- users.register(reg)
        adId   <- serv.create(userId, createAd)
        _      <- serv.addTag(adId, tag, userId)
        _      <- serv.removeTag(adId, tag, userId)
        ad     <- serv.find(adId)
      } yield expect.all(ad.tags === Set())
    }
  }
}
