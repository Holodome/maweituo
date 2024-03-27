package com.holodome.utils.repositories

import cats.data.OptionT
import cats.effect.Sync
import com.holodome.domain.images._
import com.holodome.repositories.ImageRepository

import scala.collection.concurrent.TrieMap

final class InMemoryImageRepository[F[_]: Sync] extends ImageRepository[F] {

  private val map = new TrieMap[ImageId, Image]()

  override def create(image: Image): F[Unit] =
    Sync[F].delay(map.put(image.id, image))

  override def getMeta(imageId: ImageId): OptionT[F, Image] =
    OptionT(Sync[F].delay(map.get(imageId)))

  override def delete(imageId: ImageId): F[Unit] =
    Sync[F].delay(map.remove(imageId))
}
