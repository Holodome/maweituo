package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.PostgresAdRepo
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.{adGen, userGen}
import maweituo.tests.resources.*
import maweituo.tests.utils.given

import doobie.util.transactor.Transactor
import weaver.*
import weaver.scalacheck.Checkers

class PostgresAdRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def adsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO]) => F[Expectations]) =
    test(name) { postgres =>
      val users = PostgresUserRepo.make(postgres)
      val ads   = PostgresAdRepo.make(postgres)
      fn(users, ads)
    }

  private val userAdGen =
    for
      u   <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
    yield u -> ad

  adsTest("create and find") { (users, ads) =>
    forall(userAdGen) { (u, ad) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        x <- ads.find(ad.id).value
      yield expect.same(Some(ad), x)
    }
  }

  adsTest("delete") { (users, ads) =>
    forall(userAdGen) { (u, ad) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- ads.delete(ad.id)
        x <- ads.find(ad.id).value
      yield expect.same(None, x)
    }
  }

  adsTest("mark as resolved") { (users, ads) =>
    forall(userAdGen) { (u, ad) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- ads.markAsResolved(ad.id)
        a <- ads.find(ad.id).value
      yield expect.same(Some(true), a.map(_.resolved))
    }
  }
