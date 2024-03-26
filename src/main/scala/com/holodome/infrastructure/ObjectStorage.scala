package com.holodome.infrastructure

import com.holodome.domain.images.ImageUrl
import com.holodome.infrastructure.ObjectStorage.ObjectId
import io.estatico.newtype.macros.newtype

trait ObjectStorage[F[_]] {
  def put(id: ObjectId, data: Array[Byte]): F[Unit]
  def get(id: ObjectId): F[Array[Byte]]
  def delete(id: ObjectId): F[Unit]
}

object ObjectStorage {
  @newtype case class ObjectId(value: String)

  object ObjectId {
    def fromImageUrl(domain: ImageUrl): ObjectId =
      ObjectId(domain.value)

    def toImageUrl(objectId: ObjectId): ImageUrl =
      ImageUrl(objectId.value)
  }
}
