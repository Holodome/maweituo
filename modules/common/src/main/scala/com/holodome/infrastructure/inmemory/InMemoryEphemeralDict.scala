package com.holodome.infrastructure.inmemory

import cats.data.OptionT
import cats.effect.Sync
import com.holodome.infrastructure.EphemeralDict

import scala.collection.concurrent.TrieMap

private final class InMemoryEphemeralDict[F[_]: Sync, K, V] extends EphemeralDict[F, K, V] {
  private val map = new TrieMap[K, V]

  override def store(a: K, b: V): F[Unit] =
    Sync[F].delay(map.addOne(a -> b))

  override def delete(a: K): F[Unit] =
    Sync[F].delay(map.remove(a))

  override def get(a: K): OptionT[F, V] =
    OptionT(Sync[F].delay(map.get(a)))
}

object InMemoryEphemeralDict {
  def make[F[_]: Sync, K, V]: EphemeralDict[F, K, V] =
    new InMemoryEphemeralDict()
}
