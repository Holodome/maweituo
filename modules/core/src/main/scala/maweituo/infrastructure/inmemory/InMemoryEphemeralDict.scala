package maweituo
package infrastructure
package inmemory

import scala.collection.concurrent.TrieMap

import cats.data.OptionT
import cats.effect.Sync
object InMemoryEphemeralDict:
  def make[F[_]: Sync, K, V]: EphemeralDict[F, K, V] = new:
    val map = new TrieMap[K, V]

    def store(a: K, b: V): F[Unit] =
      Sync[F].delay(map.addOne(a -> b))

    def delete(a: K): F[Unit] =
      Sync[F].delay(map.remove(a))

    def get(a: K): OptionT[F, V] =
      OptionT(Sync[F].delay(map.get(a)))
