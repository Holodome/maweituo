package com.holodome.infrastructure

import cats.data.OptionT
import com.holodome.infrastructure.ObjectStorage.OBSId
import com.holodome.infrastructure.ObjectStorage.OBSUrl
import derevo.cats.show
import derevo.derive
import io.estatico.newtype.macros.newtype

trait ObjectStorage[F[_]] {
  def makeUrl(id: OBSId): OBSUrl

  def putStream(id: OBSId, data: fs2.Stream[F, Byte], dataSize: Long): F[Unit]
  def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]]
  def delete(id: OBSId): F[Unit]

  def put(id: OBSId, data: Array[Byte]): F[Unit] =
    putStream(id, fs2.Stream.emits(data), data.length)
}

object ObjectStorage {
  @newtype case class OBSUrl(value: String)

  @derive(show)
  @newtype case class OBSId(value: String)
}
