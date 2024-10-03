package maweituo.it.postgres.repos

import cats.effect.*

import maweituo.domain.ads.repos.{AdRepo, AdTagRepo}
import maweituo.domain.users.repos.UserRepo
import maweituo.it.resources.PostgresContainerResource.PgCon
import maweituo.postgres.ads.repos.{PostgresAdRepo, PostgresAdTagRepo}
import maweituo.postgres.repos.users.PostgresUserRepo
import maweituo.tests.generators.{adGen, adTagGen, userGen}
import maweituo.tests.utils.given
import maweituo.tests.{ResourceSuite, WeaverLogAdapter}

import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import weaver.*
import weaver.scalacheck.Checkers

class PostgresAdTagRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.getOrFailR[PgCon]().map(_.xa)

  private def tagsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdTagRepo[IO]) => F[Expectations]) =
    test(name) { (postgres, log) =>
      given Logger[IO] = new WeaverLogAdapter[IO](log)
      val users        = PostgresUserRepo.make(postgres)
      val ads          = PostgresAdRepo.make(postgres)
      val tags         = PostgresAdTagRepo.make(postgres)
      fn(users, ads, tags)
    }

  private val gen =
    for
      user <- userGen
      ad0  <- adGen
      ad = ad0.copy(authorId = user.id)
      tag <- adTagGen
    yield (user, ad, tag)

  tagsTest("and tag and get") { (users, ads, tags) =>
    forall(gen) { (user, ad, tag) =>
      for
        _ <- users.create(user)
        _ <- ads.create(ad)
        _ <- tags.addTagToAd(ad.id, tag)
        x <- tags.getAdTags(ad.id)
      yield expect.same(List(tag), x)
    }
  }

  tagsTest("remove tag") { (users, ads, tags) =>
    forall(gen) { (user, ad, tag) =>
      for
        _ <- users.create(user)
        _ <- ads.create(ad)
        _ <- tags.addTagToAd(ad.id, tag)
        _ <- tags.removeTagFromAd(ad.id, tag)
        x <- tags.getAdTags(ad.id)
      yield expect.same(List(), x)
    }
  }
