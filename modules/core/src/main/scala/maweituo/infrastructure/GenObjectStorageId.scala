package maweituo.infrastructure

import cats.Functor
import cats.syntax.all.*

import maweituo.effects.GenUUID
import maweituo.infrastructure.OBSId

trait GenObjectStorageId[F[_]]:
  def genId: F[OBSId]

object GenObjectStorageId:
  def apply[F[_]: GenObjectStorageId]: GenObjectStorageId[F] = summon

  given [F[_]: GenUUID: Functor]: GenObjectStorageId[F] with
    def genId: F[OBSId] = GenUUID[F].gen map { uuid => OBSId(uuid.toString) }
