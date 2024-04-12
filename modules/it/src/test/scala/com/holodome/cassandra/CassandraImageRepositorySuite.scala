package com.holodome.cassandra

import cats.effect.IO
import cats.syntax.all._
import cats.Show
import com.holodome.domain.errors.InvalidImageId
import com.holodome.domain.images.{Image, MediaType}
import com.holodome.generators._
import com.holodome.repositories.cassandra.CassandraImageRepository

object CassandraImageRepositorySuite extends CassandraSuite {
  private implicit val imageShow: Show[Image] = Show.show(_ => "Image")
  test("basic operations work") { cassandra =>
    val gen = for {
      id   <- imageIdGen
      adId <- adIdGen
      url  <- imageUrlGen
    } yield Image(id, adId, url, MediaType("image", "jpeg"))
    forall(gen) { image =>
      val repo = CassandraImageRepository.make[IO](cassandra)
      for {
        _ <- repo.create(image)
        i <- repo.getMeta(image.id).getOrRaise(InvalidImageId())
        _ <- repo.delete(image.id)
      } yield expect.all(i.id === image.id, i.url === image.url, i.adId === image.adId)
    }
  }
}
