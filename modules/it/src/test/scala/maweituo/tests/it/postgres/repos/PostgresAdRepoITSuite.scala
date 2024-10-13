package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.resources.*

import doobie.util.transactor.Transactor
import weaver.GlobalRead
import weaver.scalacheck.Checkers

class PostgresAdRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def adsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO]) => F[Expectations]) =
    itTest(name) { postgres =>
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
