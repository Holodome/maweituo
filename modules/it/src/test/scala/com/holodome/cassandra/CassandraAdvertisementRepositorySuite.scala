package com.holodome.cassandra

import cats.Show
import cats.effect.IO
import cats.syntax.all.*
import com.holodome.cassandra.repositories.CassandraAdRepository
import com.holodome.domain.ads.Advertisement
import com.holodome.domain.errors.InvalidAdId
import com.holodome.tests.generators.adGen

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
