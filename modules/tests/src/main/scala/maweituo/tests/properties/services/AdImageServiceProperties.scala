package maweituo.tests.properties.services

import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.ads.services.AdService
import maweituo.domain.errors.*
import maweituo.domain.services.AdImageService
import maweituo.domain.users.services.UserService
import maweituo.tests.generators.*

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait AdImageServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (UserService[IO], AdService[IO], AdImageService[IO]) => IO[Expectations]
  )

  protected val properties = List(
    Property(
      "create works",
      (users, ads, images) =>
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
    ),
    Property(
      "get invalid image",
      (_, _, images) =>
        forall(imageIdGen) { id =>
          for
            x <- images.get(id).attempt
          yield expect.same(Left(InvalidImageId(id)), x)
        }
    ),
    Property(
      "delete works",
      (users, ads, images) =>
        val gen =
          for
            reg <- registerGen
            ad  <- createAdRequestGen
            img <- imageContentsGen[IO]
          yield (reg, ad, img)
        forall(gen) { case (reg, createAd, imgCont) =>
          for
            u <- users.create(reg)
            a <- ads.create(u, createAd)
            i <- images.upload(u, a, imgCont)
            _ <- images.delete(i, u)
            x <- images.get(i).attempt
          yield expect.same(Left(InvalidImageId(i)), x)
        }
    ),
    Property(
      "delete by not author is forbidden",
      (users, ads, images) =>
        val gen =
          for
            reg  <- registerGen
            reg1 <- registerGen
            ad   <- createAdRequestGen
            img  <- imageContentsGen[IO]
          yield (reg, reg1, ad, img)
        forall(gen) { case (reg, reg1, createAd, imgCont) =>
          for
            u  <- users.create(reg)
            u1 <- users.create(reg1)
            a  <- ads.create(u, createAd)
            i  <- images.upload(u, a, imgCont)
            x  <- images.delete(i, u1).attempt
          yield expect.same(Left(AdModificationForbidden(a, u1)), x)
        }
    )
  )
