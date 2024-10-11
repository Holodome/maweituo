package maweituo
package tests
package repos
package inmemory

import java.time.Instant

import maweituo.utils.given

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
    val gen =
      for
        i  <- instantGen
        ad <- adGen
      yield i -> ad
    forall(gen) { (at, ad) =>
      for
        _ <- ads.create(ad)
        _ <- ads.markAsResolved(ad.id, at)
        a <- ads.find(ad.id).value
      yield expect.same(Some(true), a.map(_.resolved)) and expect.same(Some(at), a.map(_.updatedAt))
    }
  }
