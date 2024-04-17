package com.holodome.infrastructure

import cats.syntax.all._
import cats.data.OptionT
import cats.effect.Sync
import com.holodome.infrastructure.ObjectStorage._

import scala.collection.concurrent.TrieMap

final class InMemoryObjectStorage[F[_]: Sync] extends ObjectStorage[F] {

  private val map = new TrieMap[ObjectId, Array[Byte]]

  override def putStream(id: ObjectId, data: fs2.Stream[F, Byte], size: Long): F[Unit] =
    data.compile.toVector.map(_.toArray).flatMap(arr => Sync[F].delay(map.put(id, arr)))

  override def get(id: ObjectId): OptionT[F, fs2.Stream[F, Byte]] =
    OptionT(Sync[F].delay(map.get(id)))
      .map(arr => fs2.Stream.emits(arr).covary[F])

  override def delete(id: ObjectId): F[Unit] =
    Sync[F].delay(map.remove(id))

  override def makeUrl(id: ObjectId): String = ""
}
