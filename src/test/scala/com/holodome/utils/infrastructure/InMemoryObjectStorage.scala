package com.holodome.utils.infrastructure

import cats.data.OptionT
import cats.effect.Sync
import com.holodome.infrastructure.ObjectStorage
import com.holodome.infrastructure.ObjectStorage._

import scala.collection.concurrent.TrieMap

final class InMemoryObjectStorage[F[_]: Sync] extends ObjectStorage[F] {

  private val map = new TrieMap[ObjectId, Array[Byte]]

  override def put(id: ObjectId, data: Array[Byte]): F[Unit] =
    Sync[F].delay(map.put(id, data))

  override def get(id: ObjectId): OptionT[F, Array[Byte]] =
    OptionT(Sync[F].delay(map.get(id)))

  override def delete(id: ObjectId): F[Unit] =
    Sync[F].delay(map.remove(id))
}
