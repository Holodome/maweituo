package maweituo.tests.properties.services

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.ads.services.AdService
import maweituo.domain.errors.*
import maweituo.domain.users.UserId
import maweituo.domain.users.services.UserService
import maweituo.tests.generators.*

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

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
            adId   <- ads.create(userId, createAd)
            ad     <- ads.get(adId)
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
          yield expect.same(Left(InvalidAdId(id)), x)
        }
    ),
    Property(
      "delete works",
      (users, ads) =>
        forall(regAdGen) { case (reg, createAd) =>
          for
            userId <- users.create(reg)
            adId   <- ads.create(userId, createAd)
            _      <- ads.get(adId)
            _      <- ads.delete(adId, userId)
            x      <- ads.get(adId).attempt
          yield expect.same(Left(InvalidAdId(adId)), x)
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
            adId    <- ads.create(userId, createAd)
            _       <- ads.get(adId)
            x       <- ads.delete(adId, otherId).attempt
            a       <- ads.get(adId)
          yield expect.same(Left(AdModificationForbidden(adId, otherId)), x) and expect.same(a.title, createAd.title)
        }
    )
  )
