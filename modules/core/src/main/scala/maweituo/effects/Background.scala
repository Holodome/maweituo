package maweituo.effects

import cats.effect.Temporal
import cats.effect.std.Supervisor
import cats.syntax.all.*

trait Background[F[_]]:
  def schedule[A](fa: F[A]): F[Unit]

object Background:
  def apply[F[_]: Background]: Background[F] = summon

  given [F[_]](using S: Supervisor[F], T: Temporal[F]): Background[F] with
    def schedule[A](fa: F[A]): F[Unit] =
      S.supervise(fa).void
