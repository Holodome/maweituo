package maweituo.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all.*
import maweituo.cassandra.repos.CassandraAdRepository
import maweituo.domain.ads.Advertisement
import maweituo.domain.errors.InvalidAdId
import maweituo.tests.generators.adGen

object CassandraAdRepositorySuite extends CassandraSuite:
  given Show[Advertisement] = Show.show(_ => "Ad")
  test("basic operations work") { cassandra =>
    forall(adGen) { ad =>
      val repo = CassandraAdRepository.make[IO](cassandra)
      for
        _ <- repo.create(ad)
        a <- repo.find(ad.id).getOrRaise(InvalidAdId(ad.id))
        _ <- repo.delete(ad.id)
      yield expect.all(a.id === ad.id, a.authorId === ad.authorId, a.title === ad.title)
    }
  }
