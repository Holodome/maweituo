package com.holodome.infrastructure

import cats.Functor
import com.holodome.effects.GenUUID
import com.holodome.infrastructure.ObjectStorage.ObjectId
import cats.syntax.all._

object ObjectStorageIdGen {
  def make[F[_]: Functor: GenUUID]: F[ObjectId] =
    GenUUID[F].make map { uuid => ObjectId(uuid.toString) }
}
