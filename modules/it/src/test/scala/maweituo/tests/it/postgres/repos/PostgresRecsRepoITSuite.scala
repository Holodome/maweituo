package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*

import weaver.GlobalRead

class PostgresRecsRepoITSuite(global: GlobalRead) extends PostgresITSuite(global):

  private def recsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdTagRepo[IO], RecsRepo[IO]) => F[Expectations]) =
    pgTest(name) { postgres =>
      val users = PostgresUserRepo.make(postgres)
      val ads   = PostgresAdRepo.make(postgres)
      val tags  = PostgresAdTagRepo.make(postgres)
      val recs  = PostgresRecsRepo.make(postgres)
      fn(users, ads, tags, recs)
    }

  recsTest("learn and get closest") { (users, ads, tags, recs) =>
    val gen =
      for
        u   <- userGen
        ad0 <- adGen
        ad = ad0.copy(authorId = u.id)
        tag <- adTagGen
      yield (u, ad, tag)
    forall(gen) { (user, ad, tag) =>
      for
        _ <- users.create(user)
        _ <- ads.create(ad)
        _ <- tags.addTagToAd(ad.id, tag)
        _ <- recs.learn
      yield success
    }
  }
