package maweituo
package infrastructure
package inmemory

import scala.collection.concurrent.TrieMap

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*

import maweituo.infrastructure.{OBSId, OBSUrl}

final class InMemoryObjectStorage[F[_]: Sync] extends ObjectStorage[F]:

  private val map = new TrieMap[OBSId, Array[Byte]]

  override def putStream(id: OBSId, data: fs2.Stream[F, Byte], size: Long): F[Unit] =
    data.compile.toVector.map(_.toArray).flatMap(arr => Sync[F].delay(map.put(id, arr)))

  override def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]] =
    OptionT(Sync[F].delay(map.get(id)))
      .map(arr => fs2.Stream.emits(arr).covary[F])

  override def delete(id: OBSId): F[Unit] =
    Sync[F].delay(map.remove(id))

  override def makeUrl(id: OBSId): OBSUrl = OBSUrl("")
