package maweituo.tests.repos.inmemory

import maweituo.tests.generators.*
import maweituo.tests.repos.*

import cats.effect.IO
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import org.scalacheck.Gen
import maweituo.domain.ads.images.Image
import maweituo.domain.ads.images.MediaType

object InMemoryAdImageRepoSuite extends SimpleIOSuite with Checkers:
  private def repo = InMemoryRepoFactory.images[IO]

  private val mediaTypeGen: Gen[MediaType] =
    for
      s1 <- nonEmptyStringGen
      s2 <- nonEmptyStringGen
    yield MediaType(s1, s2)

  private val imageGen: Gen[Image] =
    for
      id   <- imageIdGen
      adId <- adIdGen
      url  <- imageUrlGen
      m    <- mediaTypeGen
      s    <- Gen.chooseNum(10, 20)
    yield Image(id, adId, url, m, s)

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
        i1 <- imageGen
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
