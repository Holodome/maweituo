package maweituo.tests.repos.inmemory

import cats.effect.IO

import maweituo.tests.generators.*
import maweituo.tests.repos.*

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryAdRepoSuite extends SimpleIOSuite with Checkers:

  private def repo = InMemoryRepoFactory.ads[IO]

  test("create and find") {
    val ads = repo
    forall(adGen) { ad =>
      for
        _ <- ads.create(ad)
        u <- ads.find(ad.id).value
      yield expect.same(u, Some(ad))
    }
  }

  test("create and list in all") {
    forall(adGen) { ad =>
      val ads = repo
      for
        _ <- ads.create(ad)
        a <- ads.all
      yield expect.same(List(ad), a)
    }
  }

  test("create two and list in all") {
    val gen =
      for
        a1 <- adGen
        a2 <- adGen
      yield a1 -> a2
    forall(gen) { (ad1, ad2) =>
      val ads = repo
      for
        _ <- ads.create(ad1)
        _ <- ads.create(ad2)
        a <- ads.all
      yield expect.same(List(ad1, ad2).sortBy(_.id), a.sortBy(_.id))
    }
  }

  test("delete") {
    val ads = repo
    forall(adGen) { ad =>
      for
        _ <- ads.create(ad)
        _ <- ads.delete(ad.id)
        x <- ads.find(ad.id).value
      yield expect.same(None, x)
    }
  }

  test("mark as resolved") {
    val ads = repo
    forall(adGen) { ad =>
      for
        _ <- ads.create(ad)
        _ <- ads.markAsResolved(ad.id)
        a <- ads.find(ad.id).value
      yield expect.same(Some(true), a.map(_.resolved))
    }
  }
