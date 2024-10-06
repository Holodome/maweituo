package maweituo.tests.repos.inmemory

import cats.effect.IO

import maweituo.domain.ads.images.Image
import maweituo.tests.generators.{adIdGen, imageGen as imageGen0}
import maweituo.tests.repos.*

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryAdImageRepoSuite extends SimpleIOSuite with Checkers:

  private def repo = InMemoryRepoFactory.images[IO]

  private val imageGen =
    for
      id  <- adIdGen
      img <- imageGen0(id)
    yield img

  test("create and find") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        x <- images.find(img.id).value
      yield expect.same(Some(img), x)
    }
  }

  test("delete") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        _ <- images.delete(img.id)
        x <- images.find(img.id).value
      yield expect.same(None, x)
    }
  }

  test("find by ad") {
    val images = repo
    forall(imageGen) { img =>
      for
        _ <- images.create(img)
        x <- images.findIdsByAd(img.adId)
      yield expect.same(List(img.id), x)
    }
  }

  test("find two by ad") {
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
