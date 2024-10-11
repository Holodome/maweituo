package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*
import maweituo.postgres.repos.all.*
import maweituo.tests.resources.*

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.GlobalRead
import weaver.scalacheck.CheckConfig

class PostgresRecsRepoITSuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, maximumGeneratorSize = 1, perPropertyParallelism = 1)

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def recsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdTagRepo[IO], RecsRepo[IO]) => F[Expectations]) =
    itTest(name) { (postgres, log) =>
      given Logger[IO] = WeaverLogAdapter(log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
      val tags         = PostgresAdTagRepo.make(postgres)
      val recs         = PostgresRecsRepo.make(postgres)
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
