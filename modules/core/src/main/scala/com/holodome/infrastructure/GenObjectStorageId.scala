package com.holodome.infrastructure

import com.holodome.effects.GenUUID
import com.holodome.infrastructure.OBSId

import cats.Functor
import cats.syntax.all.*

trait GenObjectStorageId[F[_]]:
  def make: F[OBSId]

object GenObjectStorageId:
  def apply[F[_]: GenObjectStorageId]: GenObjectStorageId[F] = summon

  given [F[_]: GenUUID: Functor]: GenObjectStorageId[F] with
    def make: F[OBSId] = GenUUID[F].make map { uuid => OBSId(uuid.toString) }
