package maweituo
package tests
package properties
package services

import weaver.MutableIOSuite

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
            u <- users.create(reg)
            given Identity = Identity(u)
            a    <- ads.create(createAd)
            i    <- images.upload(a, imgCont)
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
          yield expect.same(Left(DomainError.InvalidImageId(id)), x)
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
            given Identity = Identity(u)
            a <- ads.create(createAd)
            i <- images.upload(a, imgCont)
            _ <- images.delete(i)
            x <- images.get(i).attempt
          yield expect.same(Left(DomainError.InvalidImageId(i)), x)
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
            given Identity = Identity(u)
            a <- ads.create(createAd)
            i <- images.upload(a, imgCont)
            x <- images.delete(i)(using Identity(u1)).attempt
          yield expect.same(Left(DomainError.AdModificationForbidden(a, u1)), x)
        }
    )
  )
