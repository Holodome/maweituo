package maweituo
package infrastructure

import maweituo.infrastructure.OBSId
import maweituo.infrastructure.effects.GenUUID

trait GenObjectStorageId[F[_]]:
  def genId: F[OBSId]

object GenObjectStorageId:
  def apply[F[_]: GenObjectStorageId]: GenObjectStorageId[F] = summon

  given [F[_]: GenUUID: Functor]: GenObjectStorageId[F] with
    def genId: F[OBSId] = GenUUID[F].gen map { uuid => OBSId(uuid.toString) }
