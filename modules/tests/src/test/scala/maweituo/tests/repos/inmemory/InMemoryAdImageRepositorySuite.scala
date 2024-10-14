package maweituo
package tests
package repos
package inmemory

import maweituo.domain.all.*
import maweituo.tests.generators.imageGen as imageGen0

object InMemoryAdImageRepoSuite extends MaweituoSimpleSuite:

  private def repo = InMemoryRepoFactory.images[IO]

  private val imageGen =
    for
      id  <- adIdGen
      img <- imageGen0(id)
    yield img

  unitTest("create and find") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        x <- images.find(img.id).value
      yield expect.same(Some(img), x)
    }
  }

  unitTest("delete") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        _ <- images.delete(img.id)
        x <- images.find(img.id).value
      yield expect.same(None, x)
    }
  }

  unitTest("find by ad") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        x <- images.findIdsByAd(img.adId)
      yield expect.same(List(img.id), x)
    }
  }

  unitTest("find two by ad") {
    val images = repo
    val gen =
      for
        i1   <- imageGen
        i2_0 <- imageGen
        i2 = i2_0.copy(adId = i1.adId)
      yield i1 -> i2
    forall(gen) { (img1, img2) =>
      for
        _ <- images.create(img1)
        _ <- images.create(img2)
        x <- images.findIdsByAd(img1.adId)
      yield expect.same(List(img1, img2).map(_.id).sorted, x.sorted)
    }
  }
