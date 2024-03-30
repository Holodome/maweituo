package com.holodome.cassandra

import cats.effect.IO
import cats.syntax.all._
import cats.Show
import com.holodome.domain.ads.{Advertisement, InvalidAdId}
import com.holodome.generators.createAdGen
import com.holodome.repositories.cassandra.CassandraAdvertisementRepository

object CassandraAdvertisementRepositorySuite extends CassandraSuite {
  implicit val adShow: Show[Advertisement] = Show.show(_ => "Ad")
  test("basic operations work") { cassandra =>
    forall(createAdGen) { ad =>
      val repo = CassandraAdvertisementRepository.make[IO](cassandra)
      for {
        _ <- repo.create(ad)
        a <- repo.find(ad.id).getOrRaise(InvalidAdId(ad.id))
        _ <- repo.delete(ad.id)
      } yield expect.all(a.id === ad.id, a.authorId === ad.authorId, a.title === ad.title)
    }
  }
}
