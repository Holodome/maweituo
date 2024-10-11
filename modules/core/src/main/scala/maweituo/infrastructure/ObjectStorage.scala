package maweituo
package infrastructure

import maweituo.domain.all.*
import maweituo.utils.Newtype

trait ObjectStorage[F[_]]:
  def makeUrl(id: OBSId): OBSUrl

  def putStream(id: OBSId, data: fs2.Stream[F, Byte], dataSize: Long): F[Unit]
  def get(id: OBSId): OptionT[F, fs2.Stream[F, Byte]]
  def delete(id: OBSId): F[Unit]

  def put(id: OBSId, data: Seq[Byte]): F[Unit] =
    putStream(id, fs2.Stream.emits(data), data.length)

type OBSUrl = OBSUrl.Type
object OBSUrl extends Newtype[String]

type OBSId = OBSId.Type
object OBSId extends Newtype[String]:
  given Conversion[OBSId, ImageUrl] = (id: OBSId) => ImageUrl(id.value)
