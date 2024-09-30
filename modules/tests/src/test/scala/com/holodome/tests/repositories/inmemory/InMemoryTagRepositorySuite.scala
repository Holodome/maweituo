package com.holodome.tests.repositories.inmemory

import com.holodome.tests.generators.*
import com.holodome.tests.repositories.*

import cats.effect.IO
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object InMemoryTagRepositorySuite extends SimpleIOSuite with Checkers:

  private def repo = InMemoryRepositoryFactory.tags[IO]

  private val tagAdGen =
    for
      tag <- adTagGen
      ad  <- adIdGen
    yield tag -> ad

  test("and tag and get") {
    val tags = repo
    forall(tagAdGen) { (tag, ad) =>
      for
        _ <- tags.addTagToAd(ad, tag)
        x <- tags.getAdTags(ad)
      yield expect.same(List(tag), x)
    }
  }

  test("and tag and get all tags") {
    forall(tagAdGen) { (tag, ad) =>
      val tags = repo
      for
        _ <- tags.addTagToAd(ad, tag)
        x <- tags.getAllTags
      yield expect.same(List(tag), x)
    }
  }

  test("remove tag") {
    val tags = repo
    forall(tagAdGen) { (tag, ad) =>
      for
        _ <- tags.addTagToAd(ad, tag)
        _ <- tags.removeTagFromAd(ad, tag)
        x <- tags.getAdTags(ad)
      yield expect.same(List(), x)
    }
  }

  test("get all ads by tag") {
    val gen =
      for
        tag <- adTagGen
        ad  <- adIdGen
        ad1 <- adIdGen
      yield (tag, ad, ad1)
    forall(gen) { (tag, ad, ad1) =>
      val tags = repo
      for
        _ <- tags.addTagToAd(ad, tag)
        _ <- tags.addTagToAd(ad1, tag)
        x <- tags.getAllAdsByTag(tag)
      yield expect.same(List(ad, ad1).sorted, x.sorted)
    }
  }
