package maweituo.effects

import java.util.UUID

import cats.ApplicativeThrow
import cats.effect.Sync

trait GenUUID[F[_]]:
  def gen: F[UUID]
  def read(str: String): F[UUID]

object GenUUID:
  def apply[F[_]: GenUUID]: GenUUID[F] = summon

  given [F[_]: Sync]: GenUUID[F] with
    def gen: F[UUID] = Sync[F].delay(UUID.randomUUID())

    def read(str: String): F[UUID] =
      ApplicativeThrow[F].catchNonFatal(UUID.fromString(str))
