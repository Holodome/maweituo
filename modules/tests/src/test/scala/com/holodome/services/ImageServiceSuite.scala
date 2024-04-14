package com.holodome.services

import cats.effect.IO
import com.holodome.repositories.{AdvertisementRepository, ChatRepository, ImageRepository, InMemoryAdRepository, InMemoryImageRepository, InMemoryUserRepository, TagRepository}
import com.holodome.services.{AdvertisementService, IAMService, ImageService, UserService}
import com.holodome.generators.{createAdRequestGen, imageContentsGen, registerGen}
import com.holodome.infrastructure.InMemoryObjectStorage
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.mockito.MockitoSugar.mock
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object ImageServiceSuite extends SimpleIOSuite with Checkers with MockitoSugar with MockitoCats {
  private def makeIam(
      ad: AdvertisementRepository[IO],
      images: ImageRepository[IO]
  ): IAMService[IO] =
    IAMService.make(ad, mock[ChatRepository[IO]], images)

  test("create works") {
    val gen = for {
      reg <- registerGen
      ad  <- createAdRequestGen
      img <- imageContentsGen[IO]
    } yield (reg, ad, img)
    forall(gen) { case (reg, createAd, imgCont) =>
      val userRepo  = new InMemoryUserRepository[IO]
      val adRepo    = new InMemoryAdRepository[IO]
      val imageRepo = new InMemoryImageRepository[IO]
      val os        = new InMemoryObjectStorage[IO]
      val iam       = makeIam(adRepo, imageRepo)
      val users     = UserService.make[IO](userRepo, iam)
      val ads       = AdvertisementService.make[IO](adRepo, mock[TagRepository[IO]], iam)
      val imgs      = ImageService.make[IO](imageRepo, ads, os, iam)
      for {
        u    <- users.register(reg)
        a    <- ads.create(u, createAd)
        i    <- imgs.upload(u, a, imgCont)
        data <- imgs.get(i)
        d1   <- data.data.compile.toVector
        d2   <- imgCont.data.compile.toVector
      } yield expect.all(d1 == d2)
    }
  }

}
