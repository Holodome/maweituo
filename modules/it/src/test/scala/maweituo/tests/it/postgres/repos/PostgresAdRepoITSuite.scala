package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*

import weaver.GlobalRead

class PostgresAdRepoITSuite(global: GlobalRead) extends PostgresITSuite(global):

  private def adsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO]) => F[Expectations]) =
    pgTest(name) { postgres =>
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
