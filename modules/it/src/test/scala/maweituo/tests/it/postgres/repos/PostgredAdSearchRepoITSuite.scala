package maweituo
package tests
package it
package postgres
package repos

import maweituo.domain.all.*
import maweituo.postgres.repos.PostgresRecsRepo
import maweituo.postgres.repos.ads.{PostgresAdRepo, PostgresAdSearchRepo, PostgresAdTagRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.resources.postgres

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.{CheckConfig, Checkers}

class PostgresAdSearchRepoITSuite(global: GlobalRead) extends ResourceSuite:

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1, maximumGeneratorSize = 1, perPropertyParallelism = 1)

  private val pagination = Pagination(10, 1)

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def baseTest(name: String)(fn: (
      UserRepo[IO],
      AdRepo[IO],
      AdTagRepo[IO],
      RecsRepo[IO],
      AdSearchRepo[IO]
  ) => F[Expectations]) =
    itTest(name) { (postgres, log) =>
      given Logger[IO] = WeaverLogAdapter(log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
      val tags         = PostgresAdTagRepo.make(postgres)
      val recs         = PostgresRecsRepo.make(postgres)
      val search       = PostgresAdSearchRepo.make(postgres)
      fn(users, ads, tags, recs, search)
    }

  private val gen =
    for
      user <- userGen
      ad0  <- adGen
      ad = ad0.copy(authorId = user.id)
      tag <- adTagGen
    yield (user, ad, tag)

  private def searchTest(name: String)(fn: (AdSearchRepo[IO]) => F[Expectations]) =
    baseTest(name) { (users, ads, tags, _, search) =>
      forall(gen) { (user, ad, tag) =>
        for
          _ <- users.create(user)
          _ <- ads.create(ad)
          _ <- tags.addTagToAd(ad.id, tag)
          x <- fn(search)
        yield x
      }
    }

  AdSortOrder.allBasic.foreach { order =>
    val orderName = order.show
    searchTest(s"$orderName sort order") { search =>
      search.search(AdSearchRequest(pagination, order)).map(_ => success)
    }
  }

  searchTest("filter by name") { search =>
    search.search(AdSearchRequest(pagination, AdSortOrder.default, None, Some("a"))).map(_ => success)
  }

  searchTest("filter by tags") { search =>
    search.search(AdSearchRequest(pagination, AdSortOrder.default, Some(NonEmptyList.of(AdTag("tag")))))
      .map(_ => success)
  }

  searchTest("filter by name and tags") { search =>
    search.search(AdSearchRequest(pagination, AdSortOrder.default, Some(NonEmptyList.of(AdTag("tag"))), Some("a")))
      .map(_ => success)
  }

  baseTest("get recs") { (users, ads, tags, recs, search) =>
    forall(gen) { (user, ad, tag) =>
      for
        _ <- users.create(user)
        _ <- ads.create(ad)
        _ <- tags.addTagToAd(ad.id, tag)
        _ <- recs.learn
        _ <- search.search(AdSearchRequest(pagination, AdSortOrder.Recs(user.id), None, None))
      yield success
    }
  }