package maweituo.tests.properties.services
import cats.effect.IO
import cats.syntax.all.*

import maweituo.domain.Identity
import maweituo.domain.ads.services.AdService
import maweituo.domain.users.UserId
import maweituo.domain.users.services.{UserAdsService, UserService}
import maweituo.tests.generators.*
import maweituo.tests.utils.given

import weaver.scalacheck.Checkers
import weaver.{Expectations, MutableIOSuite}

trait UserAdsServiceProperties:
  this: MutableIOSuite & Checkers =>

  protected final case class Property(
      name: String,
      exp: (UserService[IO], AdService[IO], UserAdsService[IO]) => IO[Expectations]
  )

  protected val properties = List(
    Property(
      "new user has no ads",
      (users, _, userAds) =>
        forall(registerGen) { reg =>
          for
            u   <- users.create(reg)
            ads <- userAds.getAds(u)
          yield expect.same(List(), ads)
        }
    ),
    Property(
      "user has ad after create",
      (users, ads, userAds) =>
        val gen =
          for
            r  <- registerGen
            ad <- createAdRequestGen
          yield r -> ad
        forall(gen) { (reg, ad) =>
          for
            u <- users.create(reg)
            a <- ads.create(ad)(using Identity(u))
            x <- userAds.getAds(u)
          yield expect.same(List(a), x)
        }
    ),
    Property(
      "invalid user has no ads",
      (_, _, userAds) =>
        forall(userIdGen) { id =>
          for
            x <- userAds.getAds(id)
          yield expect.same(List(), x)
        }
    )
  )
