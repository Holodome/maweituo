package com.holodome.infrastructure

import cats.data.OptionT
import com.holodome.domain.images.ImageUrl
import com.holodome.infrastructure.ObjectStorage.ObjectId
import derevo.cats.show
import derevo.derive
import io.estatico.newtype.macros.newtype

trait ObjectStorage[F[_]] {
  def put(id: ObjectId, data: Array[Byte]): F[Unit]
  def get(id: ObjectId): OptionT[F, Array[Byte]]
  def delete(id: ObjectId): F[Unit]
}

object ObjectStorage {
  @derive(show)
  @newtype case class ObjectId(value: String)

  object ObjectId {
    def fromImageUrl(domain: ImageUrl): ObjectId =
      ObjectId(domain.value)

    def toImageUrl(objectId: ObjectId): ImageUrl =
      ImageUrl(objectId.value)
  }
}
