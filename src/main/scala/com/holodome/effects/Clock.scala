package com.holodome.effects

import cats.effect.Sync

import java.time.Instant

trait Clock[F[_]] {
  def instant: F[Instant]
}

object Clock {
  def apply[F[_]: Clock]: Clock[F] = implicitly

  implicit def forSync[F[_]: Sync]: Clock[F] =
    new Clock[F] {
      override def instant: F[Instant] = Sync[F].delay(Instant.now)
    }
}
