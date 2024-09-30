package maweituo.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all.*
import maweituo.cassandra.repos.CassandraAdImageRepository
import maweituo.domain.errors.InvalidImageId
import maweituo.domain.ads.images.{ Image, MediaType }
import maweituo.tests.generators.*

object CassandraAdImageRepositorySuite extends CassandraSuite:
  private given Show[Image] = Show.show(_ => "Image")

  test("basic operations work") { cassandra =>
    val gen =
      for
        id   <- imageIdGen
        adId <- adIdGen
        url  <- imageUrlGen
      yield Image(id, adId, url, MediaType("image", "jpeg"), 0)
    forall(gen) { image =>
      val repo = CassandraAdImageRepository.make[IO](cassandra)
      for
        _ <- repo.create(image)
        i <- repo.findMeta(image.id).getOrRaise(InvalidImageId(image.id))
        _ <- repo.delete(image.id)
      yield expect.all(i.id === image.id, i.url === image.url, i.adId === image.adId)
    }
  }
