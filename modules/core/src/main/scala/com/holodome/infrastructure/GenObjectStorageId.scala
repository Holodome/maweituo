package com.holodome.infrastructure

import cats.Functor
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.ObjectStorage.ObjectId
import cats.syntax.all._

trait GenObjectStorageId[F[_]] {
  def make: F[ObjectId]
}

object GenObjectStorageId {
  def apply[F[_]: GenObjectStorageId]: GenObjectStorageId[F] = implicitly

  implicit def forGenUUID[F[_]: GenUUID: Functor]: GenObjectStorageId[F] =
    new GenObjectStorageId[F] {
      override def make: F[ObjectId] = GenUUID[F].make map { uuid => ObjectId(uuid.toString) }
    }
}
