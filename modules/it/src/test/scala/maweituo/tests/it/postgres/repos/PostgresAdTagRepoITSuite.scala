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

class PostgresAdTagRepoITSuite(global: GlobalRead) extends ResourceSuite:

  type Res = Transactor[IO]

  override def sharedResource: Resource[IO, Res] =
    global.postgres

  private def tagsTest(name: String)(fn: (UserRepo[IO], AdRepo[IO], AdTagRepo[IO]) => F[Expectations]) =
    itTest(name) { postgres =>
      val users = PostgresUserRepo.make(postgres)
      val ads   = PostgresAdRepo.make(postgres)
      val tags  = PostgresAdTagRepo.make(postgres)
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