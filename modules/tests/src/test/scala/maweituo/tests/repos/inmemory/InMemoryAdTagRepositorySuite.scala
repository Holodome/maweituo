package maweituo
package tests
package repos
package inmemory

object InMemoryAdTagRepoSuite extends MaweituoSimpleSuite:

  private def repo = InMemoryRepoFactory.tags[IO]

  private val tagAdGen =
    for
      tag <- adTagGen
      ad  <- adIdGen
    yield tag -> ad

  unitTest("and tag and get") {
    val tags = repo
    forall(tagAdGen) { (tag, ad) =>
      for
        _ <- tags.addTagToAd(ad, tag)
        x <- tags.getAdTags(ad)
      yield expect.same(List(tag), x)
    }
  }

  unitTest("and tag and get all tags") {
    forall(tagAdGen) { (tag, ad) =>
      val tags = repo
      for
        _ <- tags.addTagToAd(ad, tag)
        x <- tags.getAllTags
      yield expect.same(List(tag), x)
    }
  }

  unitTest("remove tag") {
    val tags = repo
    forall(tagAdGen) { (tag, ad) =>
      for
        _ <- tags.addTagToAd(ad, tag)
        _ <- tags.removeTagFromAd(ad, tag)
        x <- tags.getAdTags(ad)
      yield expect.same(List(), x)
    }
  }

  unitTest("get all ads by tag") {
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
