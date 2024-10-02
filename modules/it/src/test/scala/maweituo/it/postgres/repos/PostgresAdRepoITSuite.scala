package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.repos.AdRepo
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.PostgresAdRepo
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.containers.*
import maweituo.tests.generators.{adGen, userGen}
import maweituo.tests.utils.given
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.*
import weaver.scalacheck.Checkers

object PostgresAdRepoITSuite extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    given Logger[IO] = NoOpLogger[IO]
    makePostgresResource[IO]

  private def adsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO]) => F[Expectations]) =
    test(name) { (postgres, log) =>
      given Logger[IO] = new WeaverLogAdapter[IO](log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
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
