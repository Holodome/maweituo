package com.holodome.infrastructure

import cats.data.OptionT
import com.holodome.domain.images.ImageUrl
import com.holodome.infrastructure.ObjectStorage.{ObjectId, OBSUrl}
import derevo.cats.show
import derevo.derive
import io.estatico.newtype.macros.newtype

trait ObjectStorage[F[_]] {
  def makeUrl(id: ObjectId): OBSUrl

  def putStream(id: ObjectId, data: fs2.Stream[F, Byte], dataSize: Long): F[Unit]
  def get(id: ObjectId): OptionT[F, fs2.Stream[F, Byte]]
  def delete(id: ObjectId): F[Unit]

  def put(id: ObjectId, data: Array[Byte]): F[Unit] =
    putStream(id, fs2.Stream.emits(data), data.length)
}

object ObjectStorage {
  @newtype case class OBSUrl(value: String)

  @derive(show)
  @newtype case class ObjectId(value: String) {
    def toImageUrl: ImageUrl =
      ImageUrl(value)

  }

  object ObjectId {
    def fromImageUrl(domain: ImageUrl): ObjectId =
      ObjectId(domain.value)
  }
}
