package maweituo
package tests
package repos
package inmemory


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
