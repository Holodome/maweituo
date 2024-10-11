package maweituo
package tests
package properties
package services

import weaver.MutableIOSuite

trait AdServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (UserService[IO], AdService[IO]) => IO[Expectations]
  )

  private val regAdGen =
    for
      reg <- registerGen
      ad  <- createAdRequestGen
    yield reg -> ad

  protected val properties = List(
    Property(
      "create works",
      (users, ads) =>
        forall(regAdGen) { case (reg, createAd) =>
          for
            userId <- users.create(reg)
            given Identity = Identity(userId)
            adId <- ads.create(createAd)
            ad   <- ads.get(adId)
          yield NonEmptyList.of(
            expect.same(ad.title, createAd.title),
            expect.same(ad.id, adId),
            expect.same(ad.authorId, userId)
          ).reduce
        }
    ),
    Property(
      "get invalid",
      (users, ads) =>
        forall(adIdGen) { id =>
          for
            x <- ads.get(id).attempt
          yield expect.same(Left(DomainError.InvalidAdId(id)), x)
        }
    ),
    Property(
      "delete works",
      (users, ads) =>
        forall(regAdGen) { case (reg, createAd) =>
          for
            userId <- users.create(reg)
            given Identity = Identity(userId)
            adId <- ads.create(createAd)
            _    <- ads.get(adId)
            _    <- ads.delete(adId)
            x    <- ads.get(adId).attempt
          yield expect.same(Left(DomainError.InvalidAdId(adId)), x)
        }
    ),
    Property(
      "delete by other user is blocked",
      (users, ads) =>
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
            given Identity = Identity(userId)
            adId <- ads.create(createAd)
            _    <- ads.get(adId)
            x    <- ads.delete(adId)(using Identity(otherId)).attempt
            a    <- ads.get(adId)
          yield expect.same(Left(DomainError.AdModificationForbidden(adId, otherId)), x) and expect.same(
            a.title,
            createAd.title
          )
        }
    )
  )
