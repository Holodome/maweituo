package com.holodome.effects

import cats.effect.Sync

import java.time.Instant

trait TimeSource[F[_]] {
  def instant: F[Instant]
}

object TimeSource {
  def apply[F[_]: TimeSource]: TimeSource[F] = implicitly

  implicit def forSync[F[_]: Sync]: TimeSource[F] =
    new TimeSource[F] {
      override def instant: F[Instant] = Sync[F].delay(Instant.now)
    }
}
