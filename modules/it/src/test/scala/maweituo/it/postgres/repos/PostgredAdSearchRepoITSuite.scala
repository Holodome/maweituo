package maweituo.it.postgres.repos

import cats.effect.*
import cats.syntax.all.*

import maweituo.domain.ads.repos.{AdRepo, AdTagRepo}
import maweituo.domain.users.repos.UserRepo
import maweituo.postgres.repos.ads.{PostgresAdRepo, PostgresAdTagRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.ResourceSuite
import maweituo.tests.generators.{adGen, adTagGen, userGen}
import maweituo.tests.resources.*
import maweituo.tests.utils.given

import doobie.util.transactor.Transactor
import weaver.*
import weaver.scalacheck.Checkers
import maweituo.domain.ads.repos.AdSearchRepo
import maweituo.postgres.repos.ads.PostgresAdSearchRepo
import weaver.scalacheck.CheckConfig
import maweituo.domain.Pagination
import maweituo.domain.ads.AdSearchRequest
import maweituo.domain.ads.AdSortOrder
import cats.data.NonEmptyList
import maweituo.domain.ads.AdTag
import maweituo.postgres.repos.PostgresRecsRepo
import maweituo.tests.WeaverLogAdapter
import org.typelevel.log4cats.Logger
import maweituo.domain.repos.RecsRepo

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
    searchTest(f"$orderName sort order") { search =>
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

  baseTest(name) { (users, ads, tags, recs, search) =>
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
