package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.repos.{AdImageRepo, AdRepo}
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.ads.repos.{PostgresAdImageRepo, PostgresAdRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.generators.*
import maweituo.tests.resources.postgres
import maweituo.tests.utils.given
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.Checkers

class PostgresAdImageRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def imgTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdImageRepo[IO]) => F[Expectations]) =
    test(name) { (postgres, log) =>
      given Logger[IO] = new WeaverLogAdapter[IO](log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
      val images       = PostgresAdImageRepo.make(postgres)
      fn(users, ads, images)
    }

  private val gen =
    for
      u   <- userGen
      ad0 <- adGen
      ad = ad0.copy(authorId = u.id)
      img <- imageGen(ad.id)
    yield (u, ad, img)

  imgTest("create and find") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        x <- images.find(img.id).value
      yield expect.same(Some(img), x)
    }
  }

  imgTest("create and find by ad") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        x <- images.findIdsByAd(img.adId)
      yield expect.same(List(img.id), x)
    }
  }

  imgTest("delete") { (users, ads, images) =>
    forall(gen) { (u, ad, img) =>
      for
        _ <- users.create(u)
        _ <- ads.create(ad)
        _ <- images.create(img)
        _ <- images.delete(img.id)
        x <- images.find(img.id).value
      yield expect.same(None, x)
    }
  }
