package com.holodome.effects

import cats.effect.Temporal
import cats.effect.std.Supervisor
import cats.syntax.all._

trait Background[F[_]] {
  def schedule[A](fa: F[A]): F[Unit]
}

object Background {
  def apply[F[_]: Background]: Background[F] = implicitly

  implicit def bgInstance[F[_]](implicit S: Supervisor[F], T: Temporal[F]): Background[F] =
    new Background[F] {
      override def schedule[A](fa: F[A]): F[Unit] =
        S.supervise(fa).void
    }
}
