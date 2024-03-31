package com.holodome.infrastructure

import cats.Functor
import cats.syntax.all._
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.ObjectStorage.ObjectId

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
